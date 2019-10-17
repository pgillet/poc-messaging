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

```
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
```
