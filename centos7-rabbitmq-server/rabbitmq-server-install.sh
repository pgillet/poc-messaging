#!/bin/bash

# sudo-enabled user on the VM: vagrant/vagrant

HOSTNAME=$(hostname --short)
FQDN=$(hostname --fqdn)

# Determine the BIOS vendor
VENDOR=$(dmidecode -s bios-vendor)

# See https://www.rabbitmq.com/install-rpm.html

# Install pygpgme, a package which allows yum to handle gpg signatures, 
# and a package called yum-utils which contains the tools you need for installing source RPMs 
#yum -y -q install pygpgme yum-utils

cat <<EOM > /etc/yum.repos.d/rabbitmq_erlang.repo
[rabbitmq_erlang]
name=rabbitmq_erlang
baseurl=https://packagecloud.io/rabbitmq/erlang/el/6/x86_64
repo_gpgcheck=1
gpgcheck=0
enabled=1
gpgkey=https://packagecloud.io/rabbitmq/erlang/gpgkey
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
metadata_expire=300

[rabbitmq_erlang-source]
name=rabbitmq_erlang-source
baseurl=https://packagecloud.io/rabbitmq/erlang/el/6/SRPMS
repo_gpgcheck=1
gpgcheck=0
enabled=1
gpgkey=https://packagecloud.io/rabbitmq/erlang/gpgkey
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
metadata_expire=300
EOM

yum -q makecache -y --disablerepo='*' --enablerepo='rabbitmq_erlang'

# Install Erlang (Zero-dependency from RabbitMQ)
yum -y -q install erlang

cat <<EOM > /etc/yum.repos.d/rabbitmq_rabbitmq-server.repo
[rabbitmq_rabbitmq-server]
name=rabbitmq_rabbitmq-server
baseurl=https://packagecloud.io/rabbitmq/rabbitmq-server/el/7/x86_64
repo_gpgcheck=1
gpgcheck=0
enabled=1
gpgkey=https://packagecloud.io/rabbitmq/rabbitmq-server/gpgkey
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
metadata_expire=300

[rabbitmq_rabbitmq-server-source]
name=rabbitmq_rabbitmq-server-source
baseurl=https://packagecloud.io/rabbitmq/rabbitmq-server/el/7/SRPMS
repo_gpgcheck=1
gpgcheck=0
enabled=1
gpgkey=https://packagecloud.io/rabbitmq/rabbitmq-server/gpgkey
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
metadata_expire=300
EOM

yum -q makecache -y --disablerepo='*' --enablerepo='rabbitmq_rabbitmq-server'

# Install RabbitMQ Server
# import the new PackageCloud key that will be used starting December 1st, 2018 (GMT)
rpm --import https://packagecloud.io/rabbitmq/rabbitmq-server/gpgkey

# import the old PackageCloud key that will be discontinued on December 1st, 2018 (GMT)
rpm --import https://packagecloud.io/gpg.key

yum -y -q install rabbitmq-server

# Configuring RabbitMQ
# Controlling system limits with systemd
cat <<EOM > /etc/systemd/system/rabbitmq-server.service.d/limits.conf
[Service]
LimitNOFILE=64000
EOM

# Enable plugins
#echo "[rabbitmq_management,rabbitmq_mqtt,rabbitmq_peer_discovery_consul]." | sudo tee /etc/rabbitmq/enabled_plugins
echo "[rabbitmq_management,rabbitmq_mqtt,rabbitmq_peer_discovery_consul]." > /etc/rabbitmq/enabled_plugins

# Clustering - Peer discovery using Consul
cat <<EOM > /etc/rabbitmq/rabbitmq.conf
cluster_formation.peer_discovery_backend = rabbit_peer_discovery_consul
cluster_formation.consul.host = consul
EOM

# Erlang cookie
# Shared secret that must be present on all cluster nodes for inter-node network connectivity and authentication
# TODO: Use Consul key-value store to manage the Erlang cookie?

if [ "${VENDOR}" = Google ]; then
	# Google Cloud Platform

	METADATA_REQUEST=http://metadata.google.internal/computeMetadata/v1/instance/attributes/ERLANG_COOKIE
	HTTP_CODE=$(curl --write-out %{http_code} --output /dev/null -s -H "Metadata-Flavor: Google" ${METADATA_REQUEST})
	if [ "${HTTP_CODE}" = 200 ]; then
		ERLANG_COOKIE=$(curl -s -H "Metadata-Flavor: Google" ${METADATA_REQUEST})
	fi
fi

if [[ -n "${ERLANG_COOKIE}" ]]; then
	echo -n ${ERLANG_COOKIE} > /var/lib/rabbitmq/.erlang.cookie
fi

# Start the server
chkconfig rabbitmq-server on
/sbin/service rabbitmq-server start


if [[ "${HOSTNAME}" == rabbitmq-server-01 ]]; then 
	# Access control
	# Create a RabbitMQ user called "meteofr"
	rabbitmqctl add_user meteofr meteofr

	# Create a new virtual host called "test"
	rabbitmqctl add_vhost test

	# Grant the user named "meteofr" access to the virtual host called "test", 
	# with configure permissions on all resources whose names starts with "meteofr-", 
	# and write and read permissions on all resources
	rabbitmqctl set_permissions -p test meteofr "^meteofr-.*" ".*" ".*"

	# Tag the user with "administrator" for full management UI and HTTP API access
	rabbitmqctl set_user_tags meteofr administrator

	# Delete the defaut guest user as a primary security measure
	rabbitmqctl delete_user guest
else 
	# Clustering
	rabbitmqctl stop_app
	rabbitmqctl join_cluster rabbit@rabbitmq-server-01
	rabbitmqctl start_app
fi

# Check on service status as observed by service manager
service rabbitmq-server status

# Runs the precheck tool to scan for any compatibility issues that might cause the 
# import process to fail or the disk to not work properly on Google Compute Engine
#curl https://storage.googleapis.com/compute-image-tools/release/linux/import_precheck --output import_precheck
#chmod u+x import_precheck
#./import_precheck
