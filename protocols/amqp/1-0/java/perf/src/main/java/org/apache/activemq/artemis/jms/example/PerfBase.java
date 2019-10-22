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

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.activemq.artemis.utils.TokenBucketLimiter;
import org.apache.activemq.artemis.utils.TokenBucketLimiterImpl;
import org.apache.qpid.jms.JmsConnectionFactory;

public abstract class PerfBase {

   private static final Logger log = Logger.getLogger(PerfSender.class.getName());

   private static final String DEFAULT_PERF_PROPERTIES_FILE_NAME = "target/classes/perf.properties";

   private static byte[] randomByteArray(final int length) {
      byte[] bytes = new byte[length];

      Random random = new Random();

      for (int i = 0; i < length; i++) {
         bytes[i] = Integer.valueOf(random.nextInt()).byteValue();
      }

      return bytes;
   }

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

   private final PerfParams perfParams;

   protected PerfBase(final PerfParams perfParams) {
      this.perfParams = perfParams;
   }

    private ConnectionFactory factory;

   private Connection connection;

   private Destination destination;

   private long start;

   private Random rand = new Random();

   private void init() throws Exception {

       // If SSL/TLS
       if(perfParams.getUri().startsWith("amqps")) {
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
      }

   }

   private void displayAverage(final long numberOfMessages, final long start, final long end) {
      double duration = (1.0 * end - start) / 1000; // in seconds
      double average = 1.0 * numberOfMessages / duration;
      PerfBase.log.info(String.format("average: %.2f msg/s (%d messages in %2.2fs)", average, numberOfMessages, duration));
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
         new Producer(perfParams.isReuseConnection() ? connection : null, true).run();
         PerfBase.log.info("warmed up");

         ProducerService svc = new ProducerService(perfParams.getNumProducers(), perfParams.getNumProducers());
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

         ConsumerService svc = new ConsumerService(perfParams.getNumConsumers(), perfParams.getNumConsumers());
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

    class ProducerService implements Runnable {
        private final ExecutorService pool;
        private final int numProducers;

        public ProducerService(int numProducers, int poolSize)
                throws IOException {
            this.numProducers = numProducers;
            pool = Executors.newFixedThreadPool(poolSize);
        }

        @Override
        public void run() { // run the service
            try {
                for (int i=0; i<numProducers; i++) {
                    pool.execute(new Producer(perfParams.isReuseConnection() ? connection : null,false));
                }
            } catch (JMSException e) {
                e.printStackTrace();
                pool.shutdown();
            }
        }
    }

    class ConsumerService implements Runnable {
        private final ExecutorService pool;
        private final int numConsumers;

        public ConsumerService(int numConsumers, int poolSize)
                throws IOException {
            this.numConsumers = numConsumers;
            pool = Executors.newFixedThreadPool(poolSize);
        }

        @Override
        public void run() { // run the service
            try {
                for (int i=0; i<numConsumers; i++) {
                    pool.execute(new Consumer(perfParams.isReuseConnection() ? connection : null));
                }
            } catch (JMSException e) {
                e.printStackTrace();
                pool.shutdown();
            }
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

    class Producer implements Runnable {

        private boolean warmingUp;
        private Connection connection;
        private Session session;

        Producer(Connection connection, boolean warmingUp) throws JMSException {
            this.connection = connection != null ? connection : factory.createConnection();
            this.warmingUp = warmingUp;
            session = this.connection.createSession(perfParams.isSessionTransacted(), perfParams.isDupsOK() ? Session.DUPS_OK_ACKNOWLEDGE : Session.AUTO_ACKNOWLEDGE);
        }

        @Override
        public void run() {
            try {
                start = System.currentTimeMillis();
                sendMessages(warmingUp ? perfParams.getNoOfWarmupMessages() : perfParams.getNoOfMessagesToSend(),
                        perfParams.getBatchSize(),
                        perfParams.isDurable(),
                        perfParams.isSessionTransacted(), !warmingUp, perfParams.getThrottleRate(), perfParams.getMessageSize());
                long end = System.currentTimeMillis();
                displayAverage(warmingUp ? perfParams.getNoOfWarmupMessages() : perfParams.getNoOfMessagesToSend(), start, end);
            } catch (InterruptedException e) {
                PerfBase.log.log(Level.SEVERE, e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                PerfBase.log.log(Level.SEVERE, e.getMessage(), e);
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                /*if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }*/
            }
        }

        private void sendMessages(final int numberOfMessages,
                                  final int txBatchSize,
                                  final boolean durable,
                                  final boolean transacted,
                                  final boolean display,
                                  final int throttleRate,
                                  final int messageSize) throws Exception {
            MessageProducer producer = session.createProducer(destination);

            producer.setDeliveryMode(perfParams.isDurable() ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);

            producer.setDisableMessageID(perfParams.isDisableMessageID());

            producer.setDisableMessageTimestamp(perfParams.isDisableTimestamp());

            if (perfParams.getNumPriorities() > 0){
                int priority = rand.nextInt(perfParams.getNumPriorities());
                producer.setPriority(priority);
            }

            BytesMessage message = session.createBytesMessage();

            byte[] payload = PerfBase.randomByteArray(messageSize);

            message.writeBytes(payload);

            final int modulo = 2000;

            TokenBucketLimiter tbl = throttleRate != -1 ? new TokenBucketLimiterImpl(throttleRate, false) : null;

            boolean committed = false;
            for (int i = 1; i <= numberOfMessages; i++) {
                producer.send(message);

                if (transacted) {
                    if (i % txBatchSize == 0) {
                        session.commit();
                        committed = true;
                    } else {
                        committed = false;
                    }
                }

                if (display && i % modulo == 0) {
                    double duration = (1.0 * System.currentTimeMillis() - start) / 1000;
                    PerfBase.log.info(String.format("sent %6d messages in %2.2fs", i, duration));
                }

                if (tbl != null) {
                    tbl.limit();
                }
            }
            if (transacted && !committed) {
                session.commit();
            }
        }
    }

    class Consumer implements Runnable {

        private Session session;
        private Connection connection;

        Consumer(Connection connection) throws JMSException {
            this.connection = connection != null ? connection : factory.createConnection();
            session = this.connection.createSession(perfParams.isSessionTransacted(), perfParams.isDupsOK() ? Session.DUPS_OK_ACKNOWLEDGE : Session.AUTO_ACKNOWLEDGE);
        }

        @Override
        public void run() {
            try {
                start = System.currentTimeMillis();
                MessageConsumer consumer = session.createConsumer(destination);

                connection.start();

                CountDownLatch countDownLatch = new CountDownLatch(1);
                PerfListener listener = new PerfListener(session, countDownLatch, perfParams);
                consumer.setMessageListener(listener);
                countDownLatch.await();
                long end = System.currentTimeMillis();
                // start was set on the first received message
                displayAverage(perfParams.getNoOfMessagesToSend(), start, end);
                listener.displayAverageLatency();
            } catch (InterruptedException e) {
                PerfBase.log.log(Level.SEVERE, e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch(Exception e) {
                PerfBase.log.log(Level.SEVERE, e.getMessage(), e);
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                /*if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }*/
            }
        }
    }

   private class PerfListener implements MessageListener {

       private final Session session;

      private final CountDownLatch countDownLatch;

      private final PerfParams perfParams;

      private boolean warmingUp = true;

      private boolean started = false;

      private final int modulo;

      private final AtomicLong count = new AtomicLong(0);

      private final AtomicLong sumOfLatencies = new AtomicLong(0);

      private PerfListener(final Session session, final CountDownLatch countDownLatch, final PerfParams perfParams) {
          this.session = session;
         this.countDownLatch = countDownLatch;
         this.perfParams = perfParams;
         warmingUp = perfParams.getNoOfWarmupMessages() > 0;
         modulo = 2000;
      }

      @Override
      public void onMessage(final Message message) {
         try {
            // Time taken for a sent message to be received
            if (!perfParams.isDisableTimestamp()) {
               sumOfLatencies.addAndGet(System.currentTimeMillis() - message.getJMSTimestamp());
            }

            if (warmingUp) {
               boolean committed = checkCommit();
               if (count.incrementAndGet() == perfParams.getNoOfWarmupMessages()) {
                  PerfBase.log.info("warmed up after receiving " + count.longValue() + " msgs");
                  if (!committed) {
                     checkCommit();
                  }
                  warmingUp = false;
               }
               return;
            }

            if (!started) {
               started = true;
               // reset count to take stats
               count.set(0);
               start = System.currentTimeMillis();
            }

            long currentCount = count.incrementAndGet();
            // System.out.println("Priority = " + message.getJMSPriority());

            // TODO
            // System.out.println(currentCount);

            if (currentCount % modulo == 0) {
               double duration = (1.0 * System.currentTimeMillis() - start) / 1000;
               PerfBase.log.info(String.format("received %6d messages in %2.2fs", currentCount, duration));
               displayAverageLatency();
            }

             boolean committed = checkCommit();
             if (currentCount == perfParams.getNoOfMessagesToSend()) {
                 if (!committed) {
                     checkCommit();
                 }
                 countDownLatch.countDown();
             }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      public void displayAverageLatency() {
         if (!perfParams.isDisableTimestamp()) {
            double avgLatency = (1.0 * sumOfLatencies.get()) / (count.get() * 1000);
            PerfBase.log.info(String.format("Average time taken for a sent message to be received is %2.2fs", avgLatency));
         }
      }

      private boolean checkCommit() throws Exception {
         if (perfParams.isSessionTransacted()) {
            if (count.longValue() % perfParams.getBatchSize() == 0) {
               session.commit();

               return true;
            }
         }
         return false;
      }
   }

}
