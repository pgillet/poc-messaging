Create firewall rule:

gcloud compute --project=meteofrance-poc-messaging firewall-rules create artemis --direction=INGRESS --priority=1000 --network=default --action=ALLOW --rules=tcp:8161 --source-ranges=0.0.0.0/0 --target-tags=artemis-broker



Create template:

gcloud beta compute --project=meteofrance-poc-messaging instance-templates create artemis-broker-template --machine-type=n1-standard-1 --network=projects/meteofrance-poc-messaging/global/networks/default --network-tier=PREMIUM --metadata=enable-oslogin=TRUE --maintenance-policy=MIGRATE --service-account=ansible-sa@meteofrance-poc-messaging.iam.gserviceaccount.com --scopes=https://www.googleapis.com/auth/cloud-platform --tags=artemis-broker --image=centos-7-v20190905 --image-project=centos-cloud --boot-disk-size=10GB --boot-disk-type=pd-standard --boot-disk-device-name=artemis-broker-template --labels=group=artemis-servers,server-type=slave --reservation-affinity=any


Ansible prerequisistes:

https://cloud.google.com/compute/docs/instances/managing-instance-access
https://alex.dzyoba.com/blog/gcp-ansible-service-account/


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

Copy the default Trust Store from Java:

```
cp $JAVA_HOME/lib/security/cacerts /somewhere
```

Add the server certificate:

```
keytool -import -alias server1 -file /path/to/server_certificate.pem -keystore /somewhere/cacerts -storepass changeit
```

Add an appropriate `acceptor` with SSL enabled in `${BROKER_HOME}/etc/broker.xml`. For example, for AMQP:

```xml
<acceptor name="amqp">tcp://host:5672</acceptor>

<acceptor name="amqps">tcp://host:5671?sslEnabled=true;keyStorePath=/path/to/server_key.p12;keyStorePassword=bunnies;trustStorePath=/path/to/cacerts;trustStorePassword=changeit</acceptor>
```
Note the difference with the original acceptor without SSL.
Check also that the files have the appropriate read permission.

See [TLS Support](https://www.rabbitmq.com/ssl.html) and [Troubleshooting TLS-enabled Connections](https://www.rabbitmq.com/troubleshooting-ssl.html) for more information.

See an example of the needed configuration on the client side in the [perf](https://git.meteo.fr/poc_amqp/poc_amqp/tree/master/protocols/amqp/1-0/java/perf) project.
