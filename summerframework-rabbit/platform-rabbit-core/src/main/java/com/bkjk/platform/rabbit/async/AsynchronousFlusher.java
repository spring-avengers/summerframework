package com.bkjk.platform.rabbit.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousFlusher<T> {

    public interface Handler<T> {
        void batch(Collection<T> collection);

        default void onQueueFull(T t) {

        }
    }

    private static class Worker<T> {

        final Handler handler;
        final ArrayBlockingQueue<T> queue;
        ExecutorService executor;
        final ReentrantLock lock = new ReentrantLock();
        boolean running = false;
        final int batchSize;
        int capacity;

        public Worker(Handler handler, int batchSize, int capacity) {
            this.queue = new ArrayBlockingQueue<>(capacity);
            this.capacity = capacity;
            this.handler = handler;
            this.batchSize = batchSize;
        }

        public void add(T t) {
            if (!queue.offer(t)) {
                handler.onQueueFull(t);
            }
        }

        public void start() {
            lock.lock();
            if (this.running) {
                return;
            }
            this.running = true;
            try {
                executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    logger.info("Worker[{}] is starting. capacity = {}. batchSize = {}", Worker.this.hashCode(),
                        capacity, batchSize);
                    while (running || queue.size() > 0) {
                        try {

                            int currentBatch = Math.min(queue.size(), batchSize);
                            if (currentBatch > 0) {
                                ArrayList<T> list = new ArrayList<>(currentBatch);
                                for (int i = 0; i < currentBatch; i++) {
                                    try {
                                        list.add(queue.poll(10, TimeUnit.MILLISECONDS));
                                    } catch (Throwable t) {
                                        logger.error(t.getMessage(), t);
                                    }
                                }
                                handler.batch(list);
                            } else {
                                handler.batch(Arrays.asList(queue.take()));
                            }
                        } catch (Throwable ignore) {
                            logger.warn(ignore.getMessage(), ignore);
                        }
                    }
                    logger.info("Worker[{}] was stopped", Worker.this.hashCode());
                });
            } finally {
                lock.unlock();
            }
        }

        public void stop() {
            lock.lock();
            if (!this.running) {
                return;
            }
            this.running = false;
            try {
                if (executor != null) {
                    executor.shutdown();
                    executor = null;
                }
            } finally {
                lock.unlock();
            }
        }

    }

    public static final Logger logger = LoggerFactory.getLogger(AsynchronousFlusher.class);

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger("test.log");
        AsynchronousFlusher<String> asynchronousFlusher = new AsynchronousFlusher<>(new Handler<String>() {
            @Override
            public void batch(Collection<String> collection) {
                try {
                    TimeUnit.MILLISECONDS.sleep(collection.size());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("size={}", collection.size());
            }

            @Override
            public void onQueueFull(String s) {
                logger.info("Queue is full. {}", s);
            }
        }, 2, 1000, 100);

        asynchronousFlusher.stop();
        asynchronousFlusher.stop();
        asynchronousFlusher.start();
        asynchronousFlusher.start();
        asynchronousFlusher.start();
        for (int i = 0; i < 2000; i++) {
            asynchronousFlusher.add("s." + i);
        }
        asynchronousFlusher.stop();
    }

    final int queueCapacity;
    boolean running = false;
    final int batchSize;

    int workerCount;

    final Handler handler;

    final Thread shutdownHook = new Thread(() -> {
        AsynchronousFlusher.this.stop();
    });

    ReentrantLock lock = new ReentrantLock();

    ArrayList<Worker<T>> workers = new ArrayList<>();

    ModuloGetter moduloGetter = ModuloGetter.newModuloGetter();

    public AsynchronousFlusher(Handler<T> handler) {
        this(handler, 1, 2000, 100);
    }

    public AsynchronousFlusher(Handler<T> handler, int workerCount, int queueCapacity, int batchSize) {
        this.queueCapacity = queueCapacity;
        this.handler = handler;
        this.batchSize = batchSize;
        this.workerCount = workerCount;
        for (int i = workers.size(); i < this.workerCount; i++) {
            workers.add(new Worker<>(handler, batchSize, queueCapacity));
        }
    }

    public void add(T t) {
        getNextWorker().add(t);
    }

    private Worker<T> getNextWorker() {
        return workers.get(moduloGetter.getNext(workerCount));
    }

    public void start() {
        lock.lock();
        if (running) {
            return;
        }
        this.running = true;
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            logger.info("Starting {} workers", workers.size());
            workers.forEach(worker -> {
                worker.start();
            });
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            if (!running) {
                return;
            }
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            this.running = false;
            workers.forEach(worker -> {
                worker.stop();
            });
        } finally {
            lock.unlock();
        }
    }
}
