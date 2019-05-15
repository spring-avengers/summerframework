package com.bkjk.platform.lock;

import com.bkjk.platform.lock.concurrent.JavaConcurrentLockFactory;
import com.bkjk.platform.lock.exception.LockFailedException;
import com.bkjk.platform.lock.test.TestApplication;
import com.bkjk.platform.lock.test.TestService;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 13:19
 **/

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@Slf4j
public class TestLock {
    @Autowired
    TestService testService;

    @Autowired
    LockMonitor lockMonitor;

    @Autowired
    JavaConcurrentLockFactory javaConcurrentLockFactory;

    int runCount = 5;
    int threadCount = 20;
    ExecutorService executor;

    @Before
    public void before() throws IllegalAccessException {
        Assert.assertTrue(javaConcurrentLockFactory instanceof JavaConcurrentLockFactory);
        executor = Executors.newCachedThreadPool();
        for (String key : Arrays.asList("lockCache", "readWriteLockCache")) {
            Field field = ReflectionUtils.findField(JavaConcurrentLockFactory.class, key);
            field.setAccessible(true);
            ((ConcurrentHashMap) field.get(javaConcurrentLockFactory)).clear();
        }
    }

    @After
    public void after() throws IllegalAccessException, InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        for (String key : Arrays.asList("lockCache", "readWriteLockCache")) {
            Field field = ReflectionUtils.findField(JavaConcurrentLockFactory.class, key);
            field.setAccessible(true);
            log.info("key {}", field.get(javaConcurrentLockFactory));
        }
    }

    @Test
    public void testUpdateWithoutLock() throws InterruptedException {
        AtomicLong errorCount = new AtomicLong();
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int id = i;
            AtomicLong count = new AtomicLong();
            Thread t = new Thread(() -> {
                try {
                    while (count.incrementAndGet() < runCount) {
                        String mapKey = id % 2 + "";
//                    log.info("put {} {}",mapKey,id);
                        String result = testService.updateWithoutLock(mapKey, id);
                        if (result == null) {
                            log.error("testService.updateWithLock was null");
                        }
                        if (!result.equals(testService.format(mapKey, id))) {
                            errorCount.incrementAndGet();
                        }
                    }

                } finally {
                    countDownLatch.countDown();
                }
            });
            t.start();
        }
        countDownLatch.await();
        log.info("errorCount = {} ", errorCount);
        log.info("locks {}", lockMonitor.getLockNames());
        Assert.assertTrue(errorCount.get() > 0);
    }

    @Test
    public void testUpdateWithLock() throws InterruptedException {
        AtomicLong errorCount = new AtomicLong();
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int id = i;
            AtomicLong count = new AtomicLong();
            Thread t = new Thread(() -> {
                while (count.incrementAndGet() < runCount) {
                    String mapKey = id % 2 + "";
//                    log.info("put {} {}",mapKey,id);
                    String result = testService.updateWithLock(mapKey, id);
                    if (result == null) {
                        log.error("testService.updateWithLock was null");
                    }
                    if (!result.equals(testService.format(mapKey, id))) {
                        errorCount.incrementAndGet();
                    }
                }
                countDownLatch.countDown();
            });
            t.start();
        }
        countDownLatch.await();
        log.info("errorCount = {} ", errorCount);
        log.info("locks {}", lockMonitor.getLockNames());
        Assert.assertTrue(errorCount.get() == 0);
    }

    @Test(expected = LockFailedException.class)
    public void testTimeout() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                countDownLatch.countDown();
                testService.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        countDownLatch.await();
        Thread.sleep(50);
        testService.sleep(10000);
    }

    @Test(expected = RuntimeException.class)
    public void testException() {
        testService.exception();
    }

    @After
    public void afterException() throws IllegalAccessException {
        String key = "p.lock.com.bkjk.platform.lock.test.TestService.exception";

        Field field = ReflectionUtils.findField(JavaConcurrentLockFactory.class, "lockCache");
        field.setAccessible(true);
        ConcurrentHashMap<String, Lock> cache = (ConcurrentHashMap<String, Lock>) field.get(javaConcurrentLockFactory);
        if (cache.containsKey(key)) {
            ReentrantLock lock = (ReentrantLock) cache.get(key);
            Assert.assertTrue(!lock.isLocked());
            Assert.assertTrue(!lock.isFair());
        }
    }

    @Test
    public void testMultiKey() {
        Map<String, String> param = new HashMap<>();
        param.put("foo", "fooV");
        param.put("bar", "barV");
        testService.multiKey(param);
    }

    @Test
    public void testReadLock() throws ExecutionException, InterruptedException {
        List<Future> futureList = new ArrayList<>();
        int tc = 100;
        int sleep = 10;
        int totalTime = tc * sleep;
        long start = System.currentTimeMillis();
        for (int i = 0; i < tc; i++) {
            futureList.add(executor.submit(() -> {
                testService.readLock(sleep);
            }));
        }
        for (Future future : futureList) {
            future.get();
        }
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost < totalTime);
    }

    @Test
    public void testWriteLock() throws ExecutionException, InterruptedException {
        List<Future> futureList = new ArrayList<>();
        int tc = 100;
        int sleep = 10;
        int totalTime = tc * sleep;
        long start = System.currentTimeMillis();
        for (int i = 0; i < tc; i++) {
            futureList.add(executor.submit(() -> {
                testService.writeLock(sleep);
            }));
        }
        for (Future future : futureList) {
            future.get();
        }
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost >= totalTime);
    }


    @Test
    public void testReadWriteLock() throws ExecutionException, InterruptedException {
        List<Future> futureList = new ArrayList<>();
        int tc = 100;
        int sleep = 10;
        int totalTime = tc * sleep / 2;
        long start = System.currentTimeMillis();
        for (int i = 0; i < tc; i++) {
            if (i % 2 == 0) {
                futureList.add(executor.submit(() -> {
                    testService.writeLock(sleep);
                }));
            } else {
                futureList.add(executor.submit(() -> {
                    testService.readLock(sleep);
                }));
            }
        }
        for (Future future : futureList) {
            future.get();
        }
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost >= totalTime);
    }

    @Test
    public void testFooFallback() throws ExecutionException, InterruptedException {
        List<Future> futureList = new ArrayList<>();
        int tc = 10;
        int sleep = 100;
        int totalTime = tc * sleep;
        long start = System.currentTimeMillis();
        for (int i = 0; i < tc; i++) {
            futureList.add(executor.submit(() -> {
                testService.foo(sleep);
            }));
        }
        for (Future future : futureList) {
            future.get();
        }
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue(cost < totalTime);
    }

    @Test
    public void testBarFallback() throws InterruptedException {
        new Thread(() -> {
            testService.bar(100000);
        }).start();
        Thread.sleep(100);
        Assert.assertEquals("barFallback", testService.bar(100000));
    }

    @Test(expected = LockFailedException.class)
    public void testLockOperationTimeout() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> {
            countDownLatch.countDown();
            testService.barOperation(10000);
        }).start();
        countDownLatch.await();
        Thread.sleep(50);
        testService.barOperation(10000);
    }

    @Test
    public void testLockOperationTimeoutWithFallback() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> {
            countDownLatch.countDown();
            testService.barOperationFallback(10000);
        }).start();
        countDownLatch.await();
        Thread.sleep(50);
        Assert.assertEquals("barOperationFallback",testService.barOperationFallback(10000));
    }


    @Test
    public void testLockOperationManually() {

        for (int i = 0; i < 3; i++) {
            testService.barManually();
        }
    }

    @Test
    public void testBarOperation1(){
       Assert.assertEquals("ab",testService.barOperation1(1));
    }


}
