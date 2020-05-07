#!/bin/bash

# Install Consul server agent
# See https://learn.hashicorp.com/consul/datacenter-deploy/deployment-guide

yum install -y unzip

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

# Cloud auto-join configuration
CLOUD_AUTO_JOIN="[\"provider=gce tag_value=consul credentials_file=/etc/consul.d/key.json \"]"

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

# Server configuration
cat <<EOM > /etc/consul.d/server.hcl
server = true
bootstrap_expect = 3
ui = true
client_addr = "0.0.0.0"
EOM

chown --recursive consul:consul /etc/consul.d
chmod 640 /etc/consul.d/server.hcl

# Start Consul
systemctl enable consul
systemctl start consul
systemctl status consul

