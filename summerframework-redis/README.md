# 说明

platform-starter-redis是一个redis客户端的二次封装
* 支持Jedis和Redisson两个客户端
* 支持CacheCloud环境及非CacheCloud环境
  *  CacheCloud
  如果是在CacheCloud环境下，内部将使用依赖CacheCloud服务来实现监控上报的功能
  Tips： CacheCloud服务端地址：http://cachecloud.bkjk.cn/ ，使用前需在控制台上申请redis实例。分配应用ID，应用启动根据ID获得redis实例的连接信息
  * 非CacheCloud
  不依赖CacheCloud服务时只需在配置文件中配置好redis的连接地址即可，将结合Monitor来完成Metrcis打点来实现监控的功能

# 功能

* 快速配置一个redis客户端，支持Jedis和Redisson（默认使用Jedis）
* 支持使用CacheCloud方式来自动获取Redis实例地址
* 提供配置多个Redis实例(类似于多数据源)功能
* 提供分布式锁功能
* 提供二级缓存功能
* 客户端上报功能（限于CacheCloud)
* 集成了metrics

# 使用方式
   一般来说，在一个项目中只会有一种依赖方式，任何一种方式都提供以上功能。
##使用Jedis
### Maven引入

   '<dependency>
              <groupId>com.bkjk.platform.summerframework</groupId>
              <artifactId>platform-starter-redis</artifactId>
              <version>${latest-version}</version>
    </dependency>'
   
### 依赖CacheCloud配置Jedis
       'platform:
          cache:
            local:
              maxSize: 100
            remote:
              globalExpiration: 6000000
              jedis:
                source[0]:
                  appId: 10000
                  type: cluster'
        
### 不依赖CacheCloud配置Jedis  
    'platform:
       cache:
         local:
           maxSize: 100
         remote:
           globalExpiration: 6000000
           jedis:
             source[0]:
               cluster:
               maxRedirects: 5
               nodes: 172.29.16.71:6379,172.29.16.72:6379,172.29.16.73:6380'
### 演示代码
    '@Slf4j
     @SpringBootApplication
     @RestController
     public class CacheCloudApplication {
       private final Logger logger = LoggerFactory.getLogger(CacheCloudApplication.class);
      
       @Autowired
       private DistributedLock jedisDistributedLock;
      
       @Autowired
       private RedisTemplate redisTemplate;
      
       @Autowired
       private StringRedisTemplate stringRedisTemplate;
      
       @Autowired
       private RedisConnectionFactory redisConnectionFactory;
        
         @RequestMapping("jedis/lock")
         public String jLock() {
           for (int i = 0; i < 1; i++) {
             new JedisLockThread().start();
           }
           return "ok";
         }
          
           class JedisLockThread extends Thread {
             @Override
             public void run() {
               String key = "jedisLockKey";
               boolean result = jedisDistributedLock.tryLock(key, 1000, 10000);
               logger.info("get lock key:{}, result:{}, t:{} ", key, result, Thread.currentThread().getName());
               try {
                 Thread.sleep(5000);
               } catch (InterruptedException e) {
                 logger.error("exp", e);
               } finally {
                 jedisDistributedLock.unlock(key);
                 logger.info("release lock, key:{}, t:{} ", key, Thread.currentThread().getName());
               }
             }
           }
          
         public static void main(String[] args) {
               SpringApplication.run(CacheCloudApplication.class, args);
         }
     }
    '     

## 使用Redisson
### Maven引入
    '<dependency>
           <groupId>org.redisson</groupId>
           <artifactId>redisson</artifactId>
    </dependency>
    <dependency>
           <groupId>com.bkjk.platform.summerframework</groupId>
           <artifactId>platform-starter-redis</artifactId>
           <version>${latest-version}</version>
             <exclusions>
               <exclusion>
                 <groupId>redis.clients</groupId>
                 <artifactId>jedis</artifactId>
               </exclusion>
             </exclusions>
    </dependency>'
   
### 依赖于CacheCloud配置Redisson
       'platform:
          cache:
            local:
              maxSize: 100
            remote:
              globalExpiration: 6000000
              redisson:
                source[0]:
                  appId: 10000
                  type: cluster'
        
### 不依赖于CacheCloud配置Redisson 
    'platform:
       cache:
         local:
           maxSize: 100
         remote:
           globalExpiration: 6000000
           redisson:
             source[0]:
               config:
                clusterServersConfig: 
                 nodeAddresses: redis://172.29.16.71:6379,redis://172.29.16.72:6379,redis://172.29.16.73:6380'
### 演示代码
    '@Slf4j
     @SpringBootApplication
     @RestController
     public class RedissonTestApplication {
      private final Logger logger = LoggerFactory.getLogger(RedissonTestApplication.class);
    
      @Autowired
      private DistributedLock redissonDistributedLock;
      
      @Autowired
      private RedissonClient redissonClient;
      
       @RequestMapping("redisson/lock")
       public String rLock() {
         for (int i = 0; i < 10; i++) {
           new RedissonLockThread().start();
         }
         return "ok";
       }
      
       class RedissonLockThread extends Thread {
         @Override
         public void run() {
           String key = "redissonLockKey";
           boolean result = redissonDistributedLock.tryLock(key, 1000, 10000);
           logger.info("get lock key:{}, result:{}, t:{} ", key, result, Thread.currentThread().getName());
           try {
             Thread.sleep(5000);
           } catch (InterruptedException e) {
             logger.error("exp", e);
           } finally {
             redissonDistributedLock.unlock(key);
             logger.info("release lock, key:{}, t:{} ", key, Thread.currentThread().getName());
           }
         }
       }
        
      public static void main(String[] args) {
           SpringApplication.run(RedissonTestApplication.class, args);
      }
    }
    '
## 使用二级缓存
### 如上使用二级缓存重要的配置
* platform.cache.local.maxSize： 本地缓存（即一级缓存）保存对象的最大数量
* platform.cache.remote.globalExpiration： Redis（即二级缓存）对象失效时间，对于缓存的对象，总是应当设置一个失效时间以保证一致性
* 应用需要开启二级缓存，使用注解 `@EnableCaching`

### 二级缓存示例
`
    @Service
    public class RedisCaffeineService {
     
        private final Logger logger = LoggerFactory.getLogger(RedisCaffeineService.class);
     
        @Cacheable(key = "'cache_user_id_' + #id", value = "userIdCache", sync = true)
        public Person get(long id) {
            logger.info("get by id from db");
            Person user = new Person();
            user.setId(id);
            user.setName("name" + id);
            user.setCreateTime(new Date());
            return user;
        }
     
        @Cacheable(key = "'cache_user_name_' + #name", value = "userNameCache")
        public Person get(String name) {
            logger.info("get by name from db");
            Person user = new Person();
            user.setId(new Random().nextLong());
            user.setName(name);
            user.setCreateTime(new Date());
            return user;
        }
     
        @CachePut(key = "'cache_user_id_' + #person.id", value = "userIdCache")
        public Person update(Person person) {
            logger.info("update to db");
            person.setCreateTime(new Date());
            return person;
        }
     
        @CacheEvict(key = "'cache_user_id_' + #id", value = "userIdCache")
        public void delete(long id) {
            logger.info("delete from db");
        }
    }
`