package org.apache.activemq.artemis.jms.example;

import org.apache.activemq.artemis.utils.TokenBucketLimiter;
import org.apache.activemq.artemis.utils.TokenBucketLimiterImpl;

import javax.jms.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

class Producer implements Runnable {

    private static final Logger log = Logger.getLogger(Producer.class.getName());

    private PerfBase context;
    private PerfParams perfParams;
    private boolean warmingUp;
    private Connection connection;
    private Session session;
    private long start;
    private Random rand = new Random();
    private final int modulo;

    Producer(PerfBase context, boolean warmingUp) throws JMSException {
        this.context = context;
        this.perfParams = context.perfParams;
        this.connection = perfParams.isReuseConnection() ? context.connection : context.factory.createConnection();
        this.warmingUp = warmingUp;
        session = this.connection.createSession(perfParams.isSessionTransacted(), perfParams.isDupsOK() ? Session.DUPS_OK_ACKNOWLEDGE : Session.AUTO_ACKNOWLEDGE);
        modulo = perfParams.getThrottleRate() != -1 ? 10 : 1000;
    }

    private static byte[] randomByteArray(final int length) {
        byte[] bytes = new byte[length];

        Random random = new Random();

        for (int i = 0; i < length; i++) {
            bytes[i] = Integer.valueOf(random.nextInt()).byteValue();
        }

        return bytes;
    }

    @Override
    public void run() {
        try {
            start = System.currentTimeMillis();
            log.info(String.format("Producer %s has started", Thread.currentThread().getName()));
            sendMessages(warmingUp ? perfParams.getNoOfWarmupMessages() : perfParams.getNoOfMessagesToSend(),
                    perfParams.getBatchSize(),
                    perfParams.isDurable(),
                    perfParams.isSessionTransacted(),
                    !warmingUp,
                    warmingUp? -1: perfParams.getThrottleRate(),
                    perfParams.getMessageSize());
            long end = System.currentTimeMillis();
            displayAverage(warmingUp ? perfParams.getNoOfWarmupMessages() : perfParams.getNoOfMessagesToSend(), start, end);
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
            if (!perfParams.isReuseConnection() && connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    private void displayAverage(final long numberOfMessages, final long start, final long end) {
        double duration = (1.0 * end - start) / 1000; // in seconds
        double average = 1.0 * numberOfMessages / duration;
        log.info(String.format("[%s] average: %.2f msg/s (%d messages in %2.2fs)", Thread.currentThread().getName(), average, numberOfMessages, duration));
    }

    private void sendMessages(final int numberOfMessages,
                              final int txBatchSize,
                              final boolean durable,
                              final boolean transacted,
                              final boolean display,
                              final int throttleRate,
                              final int messageSize) throws Exception {
        MessageProducer producer = session.createProducer(context.destination);

        producer.setDeliveryMode(perfParams.isDurable() ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);

        producer.setDisableMessageID(perfParams.isDisableMessageID());

        producer.setDisableMessageTimestamp(perfParams.isDisableTimestamp());

        if (perfParams.getNumPriorities() > 0) {
            int priority = rand.nextInt(perfParams.getNumPriorities());
            producer.setPriority(priority);
        }

        TokenBucketLimiter tbl = throttleRate != -1 ? new TokenBucketLimiterImpl(throttleRate, false, perfParams.getTimeUnit(), perfParams.getUnitAmount()) : null;

        boolean committed = false;
        for (int i = 1; i <= numberOfMessages; i++) {
            BytesMessage message = session.createBytesMessage();
            byte[] payload = randomByteArray(messageSize);
            message.writeBytes(payload);

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
                log.info(String.format("[%s] sent %6d messages in %2.2fs", Thread.currentThread().getName(), i, duration));
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
