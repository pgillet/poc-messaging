The installation of a RabbitMQ node consists of a simple startup script in a CentOS VM.
See [rabbitmq-server-install.sh](rabbitmq-server-install.sh). The script was written following the [installation guide](https://www.rabbitmq.com/install-rpm.html) for CentOS.
As a prerequisite for installing RabbitMQ nodes, you must create the Consul cluster. The installation of a Consul node is also done with a startup script. See [consul-server-install.sh](consul-server-install.sh). The script was written following the [installation guide](https://learn.hashicorp.com/consul/datacenter-deploy/deployment-guide) of Consul.
It is possible to create ready-to-use CentOS 7 images for RabbitMQ and Consul directly with the help of Vagrant.

## Prerequesites
Install [Vagrant](https://www.vagrantup.com)

## Export the VM
```
vagrant package vm.gz
```

The VagrantFile is configured to upload and execute the rabbitmq-server-install.sh .
Edit the VagrantFile to run Vagrant with a different startup script. 

## Extract (Do not overwrite the Vagrantfile!)
```
tar -xvf vm.gz -C /target/directory
```


## Import the bootable virtual disk into Google Cloud
```
gcloud beta compute images import centos-7-rabbitmq-server \
    --source-file centos7-rabbitmq-server.vmdk \
    --os centos-7
```


## RabbitMQ post-installation steps

### Create a user

```
# Access control
# Create a RabbitMQ user called "dummy"
rabbitmqctl add_user dummy password

# Create a new virtual host called "test"
rabbitmqctl add_vhost test

# Grant the user named "dummy" access to the virtual host called "test", 
# with configure permissions on all resources whose names starts with "dummy-", 
# and write and read permissions on all resources
rabbitmqctl set_permissions -p test dummy "^dummy-.*" ".*" ".*"

# Tag the user with "administrator" for full management UI and HTTP API access
rabbitmqctl set_user_tags dummy administrator

# Delete the defaut guest user as a primary security measure
rabbitmqctl delete_user guest
```

### Enable TLS support

Use `tls-gen` to generate two self-signed certificates/key pairs, one for the server and one another for clients:

```
git clone https://github.com/michaelklishin/tls-gen tls-gen
cd tls-gen/basic
# private key password
make PASSWORD=bunnies
make verify
make info
ls -l ./result
```

Add the following options in `rabbitmq.conf`:

```
listeners.ssl.default = 5671

ssl_options.cacertfile = /path/to/ca_certificate.pem
ssl_options.certfile   = /path/to/server_certificate.pem
ssl_options.keyfile    = /path/to/server_key.pem
ssl_options.password   = bunnies
ssl_options.verify     = verify_peer
ssl_options.fail_if_no_peer_cert = false
```

Check that the files have the appropriate read permission.

See [TLS Support](https://www.rabbitmq.com/ssl.html) and [Troubleshooting TLS-enabled Connections](https://www.rabbitmq.com/troubleshooting-ssl.html) for more information.

See an example of the needed configuration on the client side in the [perf](https://github.com/pgillet/poc-messaging/tree/master/protocols/amqp/1-0/java/perf) project.
