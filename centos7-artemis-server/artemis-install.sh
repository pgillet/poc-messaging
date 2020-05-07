#!/bin/bash

# Install Java (without yum)
curl --silent --remote-name https://download.java.net/java/GA/jdk12/33/GPL/openjdk-12_linux-x64_bin.tar.gz
tar xf openjdk-12_linux-x64_bin.tar.gz -C /usr/local/bin/

ARTEMIS_VERSION=2.9.0

# Download ActiveMQ Artemis
curl --silent --remote-name https://archive.apache.org/dist/activemq/activemq-artemis/${ARTEMIS_VERSION}/apache-artemis-${ARTEMIS_VERSION}-bin.tar.gz

# Install Artemis
tar xf apache-artemis-${ARTEMIS_VERSION}-bin.tar.gz -C /usr/local/bin/

export JAVA_HOME=/usr/local/bin/jdk-12
ARTEMIS_HOME=/usr/local/bin/apache-artemis-${ARTEMIS_VERSION}
LOCAL_IP_ADDR=$(hostname -I)
BROKER_DIR=/var/lib/mybroker

# The first host is the live server, the others are backup servers
# Valid for shared store or replication: this is a slave server?
if [[ ${HOSTNAME} == *01 ]]; then
    SLAVE=""
else
    SLAVE="--slave"
fi

# Create a broker instance
${ARTEMIS_HOME}/bin/artemis create ${BROKER_DIR} --force --clustered --replicated ${SLAVE} --host ${LOCAL_IP_ADDR} --http-host ${LOCAL_IP_ADDR} --port-offset 0 --user guest --password guest --role guest --silent

# CORS origin
sed -i 's+^.*allow-origin.*$+<allow-origin>\*</allow-origin>+' ${BROKER_DIR}/etc/jolokia-access.xml


cat >> ${BROKER_DIR}/start.sh << EOF
#!/bin/bash
export JAVA_HOME=${JAVA_HOME}
export PATH=${JAVA_HOME}/bin:${PATH}

# Start the broker
(cd ${BROKER_DIR}/bin; ./artemis run)
EOF

chmod u+x ${BROKER_DIR}/start.sh
${BROKER_DIR}/start.sh 2>&1 > ${BROKER_DIR}/log/artemis.log &




