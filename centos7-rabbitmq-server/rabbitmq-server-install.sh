#!/bin/bash

# sudo-enabled user on the VM: vagrant/vagrant

# Install Consul client agent
# See https://learn.hashicorp.com/consul/datacenter-deploy/deployment-guide

yum install -y -q unzip

CONSUL_VERSION=1.6.0

# Download Consul
curl --silent --remote-name https://releases.hashicorp.com/consul/${CONSUL_VERSION}/consul_${CONSUL_VERSION}_linux_amd64.zip
curl --silent --remote-name https://releases.hashicorp.com/consul/${CONSUL_VERSION}/consul_${CONSUL_VERSION}_SHA256SUMS
curl --silent --remote-name https://releases.hashicorp.com/consul/${CONSUL_VERSION}/consul_${CONSUL_VERSION}_SHA256SUMS.sig

# Install Consul
unzip consul_${CONSUL_VERSION}_linux_amd64.zip
#chown root:root consul
mv consul /usr/local/bin/
#consul --version

#consul -autocomplete-install
#complete -C /usr/local/bin/consul consul

useradd --system --home /etc/consul.d --shell /bin/false consul
mkdir --parents /opt/consul
chown --recursive consul:consul /opt/consul

# Configure systemd
cat <<EOM > /etc/systemd/system/consul.service
[Unit]
Description="HashiCorp Consul - A service mesh solution"
Documentation=https://www.consul.io/
Requires=network-online.target
After=network-online.target
ConditionFileNotEmpty=/etc/consul.d/consul.hcl

[Service]
User=consul
Group=consul
ExecStart=/usr/local/bin/consul agent -config-dir=/etc/consul.d/
ExecReload=/usr/local/bin/consul reload
KillMode=process
Restart=on-failure
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOM

# Configure Consul (server)
mkdir --parents /etc/consul.d

# Cluster auto-join configuration
cat <<EOM > /etc/consul.d/consul.hcl
datacenter = "dc1"
data_dir = "/opt/consul"
encrypt = "Luj2FZWwlt8475wD1WtwUQ=="
retry_join = ["consul-01", "consul-02", "consul-03"]
performance {
  raft_multiplier = 1
}
EOM

chown --recursive consul:consul /etc/consul.d
chmod 640 /etc/consul.d/consul.hcl

# Start Consul
systemctl enable consul
systemctl start consul
systemctl status consul


# Install RabbitMQ server
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
# do compute service address
cluster_formation.consul.svc_addr_auto = true
# compute service address using node name
cluster_formation.consul.svc_addr_use_nodename = true
# use long RabbitMQ node names?
cluster_formation.consul.use_longname = true
EOM

# Erlang cookie
# Shared secret that must be present on all cluster nodes for inter-node network connectivity and authentication
# Key "rabbitmq/erlang_cookie" must be stored in Consul
ERLANG_COOKIE=$(consul kv get rabbitmq/erlang_cookie)
echo ${ERLANG_COOKIE} > /var/lib/rabbitmq/.erlang.cookie

chown rabbitmq:rabbitmq /var/lib/rabbitmq/.erlang.cookie
chmod 400 /var/lib/rabbitmq/.erlang.cookie

# Start the server
chkconfig rabbitmq-server on
/sbin/service rabbitmq-server start

# Check on service status as observed by service manager
service rabbitmq-server status

# Runs the precheck tool to scan for any compatibility issues that might cause the 
# import process to fail or the disk to not work properly on Google Compute Engine
#curl https://storage.googleapis.com/compute-image-tools/release/linux/import_precheck --output import_precheck
#chmod u+x import_precheck
#./import_precheck
