Create firewall rule:

gcloud compute --project=meteofrance-poc-messaging firewall-rules create artemis --direction=INGRESS --priority=1000 --network=default --action=ALLOW --rules=tcp:8161 --source-ranges=0.0.0.0/0 --target-tags=artemis-broker



Create template:

gcloud beta compute --project=meteofrance-poc-messaging instance-templates create artemis-broker-template --machine-type=n1-standard-1 --network=projects/meteofrance-poc-messaging/global/networks/default --network-tier=PREMIUM --metadata=enable-oslogin=TRUE --maintenance-policy=MIGRATE --service-account=ansible-sa@meteofrance-poc-messaging.iam.gserviceaccount.com --scopes=https://www.googleapis.com/auth/cloud-platform --tags=artemis-broker --image=centos-7-v20190905 --image-project=centos-cloud --boot-disk-size=10GB --boot-disk-type=pd-standard --boot-disk-device-name=artemis-broker-template --labels=group=artemis-servers,server-type=slave --reservation-affinity=any


Ansible prerequisistes:

https://cloud.google.com/compute/docs/instances/managing-instance-access
https://alex.dzyoba.com/blog/gcp-ansible-service-account/
