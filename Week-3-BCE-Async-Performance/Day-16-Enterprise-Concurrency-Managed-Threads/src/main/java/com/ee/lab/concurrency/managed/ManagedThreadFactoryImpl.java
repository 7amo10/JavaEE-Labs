package com.ee.lab.concurrency.managed;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Container-managed thread factory providing consistent naming,
 * container resource governance, and thread lifecycle registration.
 */
public class ManagedThreadFactoryImpl implements ManagedThreadFactory, ForkJoinPool.ForkJoinWorkerThreadFactory {

    private final String poolPrefix;
    private final AtomicInteger threadCounter = new AtomicInteger(1);

    public ManagedThreadFactoryImpl(String poolPrefix) {
        this.poolPrefix = poolPrefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(poolPrefix + "-worker-" + threadCounter.getAndIncrement());
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        worker.setName(poolPrefix + "-fj-worker-" + threadCounter.getAndIncrement());
        return worker;
    }
}
