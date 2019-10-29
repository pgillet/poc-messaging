/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jms.example;

import org.apache.qpid.jms.JmsConnectionFactory;

import javax.jms.*;
import javax.naming.Context;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public abstract class PerfBase {

    private static final Logger log = Logger.getLogger(PerfSender.class.getName());

    private static final String DEFAULT_PERF_PROPERTIES_FILE_NAME = "target/classes/perf.properties";

    protected final PerfParams perfParams;

    protected ConnectionFactory factory;

    protected Connection connection;

    protected Destination destination;



    protected static String getPerfFileName(final String[] args) {
        String fileName;

        if (args != null && args.length > 0) {
            fileName = args[0];
        } else {
            fileName = PerfBase.DEFAULT_PERF_PROPERTIES_FILE_NAME;
        }

        return fileName;
    }

    protected static PerfParams getParams(final String fileName) throws Exception {
        Properties props = null;

        try (InputStream is = new FileInputStream(fileName)) {
            props = new Properties();

            props.load(is);
        }

        int noOfMessages = Integer.valueOf(props.getProperty("num-messages"));
        int noOfWarmupMessages = Integer.valueOf(props.getProperty("num-warmup-messages"));
        int messageSize = Integer.valueOf(props.getProperty("message-size"));
        boolean durable = Boolean.valueOf(props.getProperty("durable"));
        boolean transacted = Boolean.valueOf(props.getProperty("transacted"));
        int batchSize = Integer.valueOf(props.getProperty("batch-size"));
        boolean drainQueue = Boolean.valueOf(props.getProperty("drain-queue"));
        String destinationName = props.getProperty("destination-name");
        int throttleRate = Integer.valueOf(props.getProperty("throttle-rate"));
        boolean dupsOK = Boolean.valueOf(props.getProperty("dups-ok-acknowledge"));
        int numPriorities = Integer.valueOf(props.getProperty("num-priorities"));
        int numProducers = Integer.valueOf(props.getProperty("num-producers"));
        int numConsumers = Integer.valueOf(props.getProperty("num-consumers"));
        boolean reuseConnection = Boolean.valueOf(props.getProperty("reuse-connection"));
        boolean disableMessageID = Boolean.valueOf(props.getProperty("disable-message-id"));
        boolean disableTimestamp = Boolean.valueOf(props.getProperty("disable-message-timestamp"));
        String clientLibrary = props.getProperty("client-library", "core");
        String uri = props.getProperty("server-uri", "tcp://localhost:61616");
        String username = props.getProperty("username");
        String password = props.getProperty("password");
        String transportKeyStoreLocation = props.getProperty("transport-keyStoreLocation");
        String transportKeyStorePassword = props.getProperty("transport-keyStorePassword");
        String transportTrustStoreLocation = props.getProperty("transport-trustStoreLocation");
        String transportTrustStorePassword = props.getProperty("transport-trustStorePassword");

        PerfBase.log.info("num-messages: " + noOfMessages);
        PerfBase.log.info("num-warmup-messages: " + noOfWarmupMessages);
        PerfBase.log.info("message-size: " + messageSize);
        PerfBase.log.info("durable: " + durable);
        PerfBase.log.info("transacted: " + transacted);
        PerfBase.log.info("batch-size: " + batchSize);
        PerfBase.log.info("drain-queue: " + drainQueue);
        PerfBase.log.info("throttle-rate: " + throttleRate);
        PerfBase.log.info("destination-name: " + destinationName);
        PerfBase.log.info("disable-message-id: " + disableMessageID);
        PerfBase.log.info("disable-message-timestamp: " + disableTimestamp);
        PerfBase.log.info("dups-ok-acknowledge: " + dupsOK);
        PerfBase.log.info("num-priorities: " + numPriorities);
        PerfBase.log.info("num-producers: " + numProducers);
        PerfBase.log.info("num-consumers: " + numConsumers);
        PerfBase.log.info("reuse-connection: " + reuseConnection);
        PerfBase.log.info("server-uri: " + uri);
        PerfBase.log.info("username: " + username);
        PerfBase.log.info("transport-keyStoreLocation: " + transportKeyStoreLocation);
        PerfBase.log.info("transport-keyStorePassword: " + transportKeyStorePassword);
        PerfBase.log.info("transport-trustStoreLocation: " + transportTrustStoreLocation);
        PerfBase.log.info("transport-trustStorePassword: " + transportTrustStorePassword);
        PerfBase.log.info("client-library:" + clientLibrary);

        PerfParams perfParams = new PerfParams();
        perfParams.setNoOfMessagesToSend(noOfMessages);
        perfParams.setNoOfWarmupMessages(noOfWarmupMessages);
        perfParams.setMessageSize(messageSize);
        perfParams.setDurable(durable);
        perfParams.setSessionTransacted(transacted);
        perfParams.setBatchSize(batchSize);
        perfParams.setDrainQueue(drainQueue);
        perfParams.setDestinationName(destinationName);
        perfParams.setThrottleRate(throttleRate);
        perfParams.setDisableMessageID(disableMessageID);
        perfParams.setDisableTimestamp(disableTimestamp);
        perfParams.setDupsOK(dupsOK);
        perfParams.setNumPriorities(numPriorities);
        perfParams.setNumProducers(numProducers);
        perfParams.setNumConsumers(numConsumers);
        perfParams.setReuseConnection(reuseConnection);
        perfParams.setLibraryType(clientLibrary);
        perfParams.setUri(uri);
        perfParams.setUsername(username);
        perfParams.setPassword(password);
        perfParams.setTransportKeyStoreLocation(transportKeyStoreLocation);
        perfParams.setTransportKeyStorePassword(transportKeyStorePassword);
        perfParams.setTransportTrustStoreLocation(transportTrustStoreLocation);
        perfParams.setTransportTrustStorePassword(transportTrustStorePassword);

        return perfParams;
    }

    protected PerfBase(final PerfParams perfParams) {
        this.perfParams = perfParams;
    }

    private void init() throws Exception {

        // If SSL/TLS
        if (perfParams.getUri().startsWith("amqps")) {
            System.setProperty("javax.net.ssl.keyStore", perfParams.getTransportKeyStoreLocation());
            System.setProperty("javax.net.ssl.keyStorePassword", perfParams.getTransportKeyStorePassword());
            System.setProperty("javax.net.ssl.trustStore", perfParams.getTransportTrustStoreLocation());
            System.setProperty("javax.net.ssl.trustStorePassword", perfParams.getTransportTrustStorePassword());
        }

        if (perfParams.isOpenwire()) {
            factory = new org.apache.activemq.ActiveMQConnectionFactory(perfParams.getUri());

            destination = new org.apache.activemq.command.ActiveMQQueue(perfParams.getDestinationName());

            connection = factory.createConnection();
        } else if (perfParams.isCore()) {
            factory = new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(perfParams.getUri());

            destination = new org.apache.activemq.artemis.jms.client.ActiveMQQueue(perfParams.getDestinationName());

            connection = factory.createConnection();

        } else if (perfParams.isAMQP()) {
            factory = new JmsConnectionFactory(perfParams.getUsername(), perfParams.getPassword(), perfParams.getUri());

            // destination = new org.apache.activemq.artemis.jms.client.ActiveMQQueue(perfParams.getDestinationName());

            connection = factory.createConnection();

            Session sessionX = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            destination = sessionX.createQueue(perfParams.getDestinationName());

            sessionX.close();

            // JNDI alternative
            /*Hashtable<Object, Object> env = new Hashtable<Object, Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
            env.put("connectionfactory.myFactoryLookup", perfParams.getUri());
            env.put("queue.myQueueLookup", perfParams.getDestinationName());
            javax.naming.Context context = new javax.naming.InitialContext(env);

            factory = (ConnectionFactory) context.lookup("myFactoryLookup");
            connection = factory.createConnection();
            destination = (Destination) context.lookup("myQueueLookup");*/
        }

    }

    protected void runSender() {
        try {
            init();

            if (perfParams.isDrainQueue()) {
                try {
                    drainQueue();
                } catch (Exception e) {
                    // Ignore: RabbitMQ throws an error when the queue is empty after draining it
                    // PerfBase.log.log(Level.SEVERE, e.getMessage(), e);
                }
            }

            PerfBase.log.info("warming up by sending " + perfParams.getNoOfWarmupMessages() + " messages");
            new Producer(this, true).run();
            PerfBase.log.info("warmed up");

            ProducerService svc = new ProducerService(this, perfParams.getNumProducers(), perfParams.getNumProducers());
            svc.run();

            awaitTerminationAfterShutdown(svc.pool);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void runListener() {
        try {
            init();

            if (perfParams.isDrainQueue()) {
                try {
                    drainQueue();
                } catch (Exception e) {
                    // Ignore: RabbitMQ throws an error when the queue is empty after draining it
                    // PerfBase.log.log(Level.SEVERE, e.getMessage(), e);
                }
            }

            connection.start();
            PerfBase.log.info("READY!!!");

            ConsumerService svc = new ConsumerService(this, perfParams.getNumConsumers(), perfParams.getNumConsumers());
            svc.run();

            awaitTerminationAfterShutdown(svc.pool);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void drainQueue() throws Exception {
        PerfBase.log.info("Draining queue");

        Session drainSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        int count = 0;
        try {
            MessageConsumer consumer = drainSession.createConsumer(destination);

            connection.start();

            Message message = null;

            do {
                message = consumer.receive(3000);

                if (message != null) {
                    message.acknowledge();

                    count++;
                }
            } while (message != null);
        } finally {
            drainSession.close();

            PerfBase.log.info("Drained " + count + " messages");
        }
    }




    public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(600, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }





}
