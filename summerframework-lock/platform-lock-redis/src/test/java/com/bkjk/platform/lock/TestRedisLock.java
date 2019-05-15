package com.bkjk.platform.lock;

import com.bkjk.platform.lock.exception.LockFailedException;
import com.bkjk.platform.lock.redis.RedisLock;
import com.bkjk.platform.lock.redis.RedisLockFactory;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 13:19
 **/

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@Slf4j
public class TestRedisLock {
    @Autowired
    TestService testService;

    @Autowired
    LockMonitor lockMonitor;


    int runCount=10;
    int threadCount=20;
    ExecutorService executor;

    @Autowired
    LockFactory lockFactory;

    String reentryLockName="reentry-lock";

    @Before
    public void before() throws IllegalAccessException {
        Assert.assertTrue(lockFactory instanceof RedisLockFactory);
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void after() throws IllegalAccessException, InterruptedException {
        ((RedisLock)lockOperation.requireLock(reentryLockName).getLock()).forceUnlock();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testUpdateWithoutLock() throws InterruptedException {
        AtomicLong errorCount=new AtomicLong();
        CountDownLatch countDownLatch=new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int id=i;
            AtomicLong count=new AtomicLong();
            Thread t=new Thread(()->{
                try{
                    while (count.incrementAndGet()<runCount){
                        String mapKey=id%2+"";
//                    log.info("put {} {}",mapKey,id);
                        String result = testService.updateWithoutLock(mapKey, id);
                        if(result==null){
                            log.error("testService.updateWithLock was null");
                        }
                        if(!result.equals(testService.format(mapKey,id))){
                            errorCount.incrementAndGet();
                        }
                    }

                }finally {
                    countDownLatch.countDown();
                }
            });
            t.start();
        }
        countDownLatch.await();
        log.info("errorCount = {} ",errorCount);
        Assert.assertTrue(errorCount.get()>0);
    }

    @Test
    public void testUpdateWithLock() throws InterruptedException {
        AtomicLong errorCount=new AtomicLong();
        CountDownLatch countDownLatch=new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int id=i;
            AtomicLong count=new AtomicLong();
            Thread t=new Thread(()->{
                try{
                    while (count.incrementAndGet()<runCount){
                        String mapKey=id%2+"";
//                    log.info("put {} {}",mapKey,id);
                        String result = testService.updateWithLock(mapKey, id);
                        if(result==null){
                            log.error("testService.updateWithLock was null");
                        }
                        if(!result.equals(testService.format(mapKey,id))){
                            errorCount.incrementAndGet();
                        }
                    }

                }finally {
                    countDownLatch.countDown();

                }
            });
            t.start();
        }
        countDownLatch.await();
        log.info("errorCount = {} ",errorCount);
        Assert.assertTrue(errorCount.get()==0);
    }

    @Test(expected= LockFailedException.class)
    public void testTimeout() throws InterruptedException {
        CountDownLatch countDownLatch=new CountDownLatch(1);
        new Thread(()->{
            try {
                countDownLatch.countDown();
                testService.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        countDownLatch.await();
        Thread.sleep(500);
        testService.sleep(1000);
    }

    @Test(expected = RuntimeException.class)
    public void testException(){
        testService.exception();
    }


    @Test
    public void testMultiKey(){
        Map<String, String> param=new HashMap<>();
        param.put("foo","fooV");
        param.put("bar","barV");
        testService.multiKey(param);
    }

    @Test(expected = ExecutionException.class)
    public void testReadLock() throws ExecutionException, InterruptedException {
        List<Future> futureList=new ArrayList<>();
        int tc=100;
        int sleep=10;
        int totalTime=tc*sleep;
        long start=System.currentTimeMillis();
        for (int i = 0; i < tc; i++) {
            futureList.add(executor.submit(()->{
               testService.readLock(sleep);
            }));
        }
        for (Future future:futureList){
            future.get();
        }
        long cost=System.currentTimeMillis()-start;
        Assert.assertTrue(cost<totalTime);
    }

    @Test(expected = ExecutionException.class)
    public void testWriteLock() throws ExecutionException, InterruptedException {
        List<Future> futureList=new ArrayList<>();
        int tc=100;
        int sleep=10;
        int totalTime=tc*sleep;
        long start=System.currentTimeMillis();
        for (int i = 0; i < tc; i++) {
            futureList.add(executor.submit(()->{
                testService.writeLock(sleep);
            }));
        }
        for (Future future:futureList){
            future.get();
        }
        long cost=System.currentTimeMillis()-start;
        Assert.assertTrue(cost>=totalTime);
    }



    @Test(expected = ExecutionException.class)
    public void testReadWriteLock() throws ExecutionException, InterruptedException {
        List<Future> futureList=new ArrayList<>();
        int tc=100;
        int sleep=10;
        int totalTime=tc*sleep/2;
        long start=System.currentTimeMillis();
        for (int i = 0; i < tc; i++) {
            if(i%2==0){
                futureList.add(executor.submit(()->{
                    testService.writeLock(sleep);
                }));
            }else {
                futureList.add(executor.submit(()->{
                    testService.readLock(sleep);
                }));
            }
        }
        for (Future future:futureList){
            future.get();
        }
        long cost=System.currentTimeMillis()-start;
        Assert.assertTrue(cost>=totalTime);
    }

    @Autowired
    private LockOperation lockOperation;

    @Test(expected = LockFailedException.class)
    public void testReentry() throws ExecutionException, InterruptedException {
        lockOperation.requireLock(reentryLockName).lockThen(lockInstance -> {
            try {
                if(!lockInstance.getLock().tryLock(500,TimeUnit.MILLISECONDS)){
                    // 不支持重入锁，所以这里必定获取不到锁
                    throw new LockFailedException();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

    }

//    @Test
//    public void testRenew(){
//        lockOperation.requireLock("testRenew").lockThen(lockInstance -> {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//    }

    @Test
    public void testSleep() throws InterruptedException {
        Thread t=new Thread(()->{
            try {
                try {
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                }
            }finally {
                System.out.println("finally done");
            }
        });
        t.start();
        Thread.sleep(1000);
        t.interrupt();
        Thread.sleep(1000);
    }
}
