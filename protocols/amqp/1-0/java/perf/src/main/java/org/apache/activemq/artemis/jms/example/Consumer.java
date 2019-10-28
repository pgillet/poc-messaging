package org.apache.activemq.artemis.jms.example;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

class Consumer implements Runnable {

    private static final Logger log = Logger.getLogger(Consumer.class.getName());

    private PerfBase context;
    private PerfParams perfParams;
    private Session session;
    private Connection connection;

    Consumer(PerfBase context) throws JMSException {
        this.context = context;
        this.perfParams = context.perfParams;
        this.connection = context.perfParams.isReuseConnection() ? context.connection : context.factory.createConnection();
        session = this.connection.createSession(perfParams.isSessionTransacted(), perfParams.isDupsOK() ? Session.DUPS_OK_ACKNOWLEDGE : Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    public void run() {
        try {
            MessageConsumer consumer = session.createConsumer(context.destination);

            connection.start();

            CountDownLatch countDownLatch = new CountDownLatch(1);
            PerfListener listener = new PerfListener(this.context, session, countDownLatch);
            consumer.setMessageListener(listener);
            countDownLatch.await();
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
                    e.printStackTrace();
                }
            }
            if (!perfParams.isReuseConnection() && connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
