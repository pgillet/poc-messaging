package org.apache.activemq.artemis.jms.example;

import javax.jms.JMSException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ProducerService implements Runnable {

    private PerfBase context;
    protected final ExecutorService pool;
    private final int numProducers;

    public ProducerService(PerfBase context, int numProducers, int poolSize)
            throws IOException {
        this.context = context;
        this.numProducers = numProducers;
        pool = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void run() { // run the service
        try {
            for (int i=0; i<numProducers; i++) {
                pool.execute(new Producer(context,false));
            }
        } catch (JMSException e) {
            e.printStackTrace();
            pool.shutdown();
        }
    }
}