package org.apache.activemq.artemis.jms.example;

import javax.jms.JMSException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ConsumerService implements Runnable {

    private PerfBase context;
    protected final ExecutorService pool;
    private final int numConsumers;

    public ConsumerService(PerfBase context, int numConsumers, int poolSize)
            throws IOException {
        this.context = context;
        this.numConsumers = numConsumers;
        pool = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void run() { // run the service
        try {
            for (int i = 0; i < numConsumers; i++) {
                pool.execute(new Consumer(this.context));
            }
        } catch (JMSException e) {
            e.printStackTrace();
            pool.shutdown();
        }
    }
}
