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

***使用时redisson分布式锁时，用户需要提供一个RedissonClient类型的bean***

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
## 用法详解

### 要点

1. name是锁的全局唯一标识，无论是内存锁还是分布式锁。任何时候，根据name获取的锁都是等价的。
2. java内置锁和redisson支持重入，redis锁未支持。只有java内置锁和redisson支持读写锁。
3. 不指定加锁超时，会永久等待
4. 分布式场景下建议使用redisson锁

### 注解加锁

通过注解来对方法进行加锁是推荐的使用方式

#### 举例

1. 修改合同信息方法需要加锁。

```java
    private final static String LOCK_NAME_MUTATE_BALANCE="mutateBalance";

    @WithLock(name = LOCK_NAME_MUTATE_BALANCE,timeoutMillis = LOCK_TIMEOUT_BALANCE,keys = "#contractUpdateDTO.id")
    public ContractUpdateDTO mutateBalance(ContractUpdateDTO contractUpdateDTO){
        // do something
        return contractUpdateDTO;
    }

```

加锁一般至少需要两个字段，锁的名字name和获取锁超时时间timeoutMillis。如果是这个例子中要根据合同ID锁定某个合同，那么还需要提供keys来告诉程序如何获取合同ID，这里支持el表达式。
 
2. 更新和查询合同两个方法，为了提高读写性能，可以把读写锁分开，更新需要加写锁，查询需要加读锁。***读写锁和第一个例子中的普通锁不能重名，同一个名字的锁只能属于一种类型的锁，否则会出现无法预期的结果！！！***

```java

    private final static ConcurrentHashMap<Long,Long> balance=new ConcurrentHashMap<>();

    private final static String LOCK_NAME_BALANCE="balance";
    private final static long LOCK_TIMEOUT_BALANCE=30_000;

    @WithLock(name = LOCK_NAME_BALANCE,timeoutMillis = LOCK_TIMEOUT_BALANCE,lockType = LockType.WRITE,keys = "#contractUpdateDTO.id")
    public long updateBalance(ContractUpdateDTO contractUpdateDTO){
        balance.put(contractUpdateDTO.getId(),balance.getOrDefault(contractUpdateDTO.getId(),0L)+contractUpdateDTO.getAmt());
        return balance.get(contractUpdateDTO.getId());
    }

    @WithLock(name = LOCK_NAME_BALANCE,timeoutMillis = LOCK_TIMEOUT_BALANCE,lockType = LockType.READ)
    public long getBalance(@LockKey Long contractUpdateDTO){
        return balance.get(contractUpdateDTO);
    }


    @Data
    public static class ContractUpdateDTO{
        private Long id;
        private long amt;
    }
```

3. 获取锁失败后降级处理
通过lockFailedFallback指定降级方法的名称（需要保证参数数量和类型完全一致）。这样如果10毫秒内没有获取到锁，会调用fooFallback方法
```java
    @WithLock(timeoutMillis = 10,lockFailedFallback = "fooFallback")
    public void foo(long s){
        sleepInner(s);
    }

    private void fooFallback(long s){
        log.info("fallback "+s);
    }
```
4. 对代码块加锁

如果因为特殊原因不能使用注解方式解锁，那么可以通过注入LockOperation来加锁，如下式对两段代码片段加锁

```java

    @Autowired
    private LockOperation lockOperation;

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
```

注意：分布式锁中通过requireLock指定相同名称连续获取多次锁，得到的是同一把锁。

## 自定义锁实现

### 数据库锁 TODO

在某些业务量不是非常大但对数据准确性要求非常严苛的场景下，redis宕机则锁无法保证准确性，
此时可以考虑牺牲一些性能来用数据库中的表模拟锁，通过数据库的强事务来保证锁的准确性。
```sql
CREATE TABLE `p_lock` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lock_keys` varchar(64) NOT NULL COMMENT '锁定的方法名',
  `memo` varchar(1024) NOT NULL COMMENT '备注信息',
  `expire` timestamp NOT NULL COMMENT '锁的失效时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建锁的时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_lock_keys` (`lock_keys`) USING BTREE
)
 ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='锁定中的方法';
 
 // 插入数据来获取锁
 insert into p_lock(lock_keys,memo,expire) values (lock_keys,memo,expire)
 
 // 获取锁的ID，后面根据ID来解锁和续期. threadLocal 
 select id from p_lock where lock_keys=lock_keys
 
 // 解锁
 delete from p_lock where id=id
 
 // 续期
 update p_lock set expire=expire+20 where id=id
```