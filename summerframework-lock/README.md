# 说明

该模块帮助用户使用注解来为方法加锁。


## 功能

* 重入锁，读写锁（redisson不支持读写锁公平锁）。
* 提供多种锁的实现，默认包含java内置锁和redisson分布式锁
* 对方法加锁（类似synchronized的功能），
* 对参数加锁（比如对用户ID进行加锁），支持spring el表达式和@LockKey注解。
* 支持加锁超时
* 支持锁有效期（仅redisson）
* 加锁或者解锁失败后支持降级到fallback方法

# 使用方式

总共提供了三种锁的实现，分别是

1. java内置锁 platform-starter-lock
2. 封装RedisTemplate实现的分布式锁（不可重入） platform-starter-lock-redis
3. 封装redisson的分布式锁 platform-starter-lock-redisson

在分布式场景下建议使用platform-starter-lock-redisson。

## Maven引入

1. 引入summerframework依赖。有两种方式

第一种是将summerframework-starter-parent作为工程的parent
```xml
	<parent>
		<groupId>com.bkjk.platform.summerframework</groupId>
		<artifactId>summerframework-starter-parent</artifactId>
		<version>LATEST_VERSION</version>
	</parent>
```

第二中是利用dependencyManagement引入依赖

```xml
<dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.bkjk.platform.summerframework</groupId>
                <artifactId>summerframework-dependencies</artifactId>
                <version>LATEST_VERSION</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
         </dependencies>
</dependencyManagement>
```

2. 引入 lock 模块
```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-lock-redisson</artifactId>
		</dependency>
```

## 演示代码

***使用时redisson分布式锁时，用户需要提供一个RedissonClient类型的bean***，使用示例可以参考[单元测试代码](./platform-lock-redisson/src/test/java/com/bkjk/platform/lock)（需要本地启动一个redis-server）

```java

public class TestService {
    private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Autowired
    private LockOperation lockOperation;

    public String format(String mapKey, long i) {
        return mapKey + " last value: " + i;
    }

    private String updateInner(String mapKey, long i) {
        cache.put(mapKey, format(mapKey, i));
        sleepInner(new Random().nextInt(20));
        return cache.get(mapKey);
    }

    @WithLock(value = "updateWithLock", keys = {"#mapKey"})
    public String updateWithLock(String mapKey, long i) {
        return updateInner(mapKey, i);
    }

    public String updateWithoutLock(String mapKey, long i) {
        return updateInner(mapKey, i);
    }

    @WithLock(timeoutMillis = 10)
    public void sleep(@LockKey long i) throws InterruptedException {
        sleepInner(i);
    }

    @WithLock
    public void exception() {
        throw new RuntimeException();
    }

    @WithLock(keys = {"#param['foo']", "#param['bar']"})
    public void multiKey(Map<String, String> param) {
        sleepInner(100);
    }

    @WithLock(name = "readWrite",lockType = LockType.READ)
    public void readLock(int s) {
        sleepInner(s);
    }

    @WithLock(name = "readWrite",lockType = LockType.WRITE)
    public void writeLock(int s) {
        sleepInner(s);
    }

    private void sleepInner(long s) {
        try {
            Thread.sleep(s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @WithLock(timeoutMillis = 10,lockFailedFallback = "fooFallback")
    public void foo(long s){
        sleepInner(s);
    }

    private void fooFallback(long s){
        log.info("fallback "+s);
    }

    @WithLock(timeoutMillis = 10,lockFailedFallback = "barFallback")
    public String bar(long s){
        sleepInner(s);
        return "bar";
    }

    private String barFallback(long s){
        log.info("fallback "+s);
        return "barFallback";
    }

    public String barOperation(long s){
        StringBuilder stringBuilder=new StringBuilder();

        LockInstance lock = lockOperation.requireLock("barOperation",10);
        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("barOperation");
        }));

        return stringBuilder.toString();
    }

    public String barOperationFallback(long s){
        StringBuilder stringBuilder=new StringBuilder();

        LockInstance lock = lockOperation.requireLock("barOperation",10);

        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("barOperation");
        }),lockInstance -> {
            // 如果加锁失败，则执行下面代码
            stringBuilder.append("barOperationFallback");
        });

        return stringBuilder.toString();
    }

    public String barOperation1(long s){
        StringBuilder stringBuilder=new StringBuilder();

        LockInstance lock = lockOperation.requireLock("barOperation1",10);
        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("a");
        }));

        // 执行业务代码，框架自动加锁和解锁
        lock.lockThen((lockInstance -> {
            sleepInner(s);
            stringBuilder.append("b");
        }));

        return stringBuilder.toString();
    }

    public void barManually(){
        Lock lock = lockOperation.requireLock("barManually").getLock();
        Assert.assertNotNull(lock);
        lock.lock();
        try {
            System.out.println("do ...");
        } finally {
            lock.unlock();
        }
    }
}
```

## 自定义锁实现

TODO
