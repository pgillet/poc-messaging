package org.apache.activemq.artemis.jms.example;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

class PerfListener implements MessageListener {

    private static final Logger log = Logger.getLogger(PerfListener.class.getName());

    private PerfBase context;
    private final Session session;

    private final CountDownLatch countDownLatch;

    private final PerfParams perfParams;

    private boolean warmingUp = true;

    private boolean started = false;

    private long start;

    private final int modulo;

    private final AtomicLong count = new AtomicLong(0);

    private final AtomicLong sumOfLatencies = new AtomicLong(0);

    private int awaitedNumberOfMessages;

    PerfListener(PerfBase context, final Session session, final CountDownLatch countDownLatch) {
        this.context = context;
        this.perfParams = context.perfParams;
        this.session = session;
        this.countDownLatch = countDownLatch;
        warmingUp = perfParams.getNoOfWarmupMessages() > 0;
        modulo = perfParams.getThrottleRate() != -1 ? 10 : 1000;
        if (perfParams.getDestinationType() == DestinationType.TOPIC) {
            awaitedNumberOfMessages = perfParams.getNoOfMessagesToSend() * perfParams.getNumProducers();
        } else {
            // Average
            awaitedNumberOfMessages = perfParams.getNoOfMessagesToSend() / perfParams.getNumConsumers();
        }
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
                    log.info("warmed up after receiving " + count.longValue() + " msgs");
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

            if (currentCount % modulo == 0 || currentCount >= awaitedNumberOfMessages) {
                double duration = (1.0 * System.currentTimeMillis() - start) / 1000;
                double average = 1.0 * currentCount / duration;
                log.info(String.format("[%s] Average: %.2f msg/s (Received 6%d messages in %2.2fs)", Thread.currentThread().getName(), average, currentCount, duration));
                if (!perfParams.isDisableTimestamp()) {
                    double avgLatency = (1.0 * sumOfLatencies.get()) / (currentCount * 1000);
                    log.info(String.format("[%s] Average time taken for a sent message to be received is %2.2fs", Thread.currentThread().getName(), avgLatency));
                }
            }

            boolean committed = checkCommit();
            if (currentCount >= awaitedNumberOfMessages) {
                if (!committed) {
                    checkCommit();
                }
                countDownLatch.countDown();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
