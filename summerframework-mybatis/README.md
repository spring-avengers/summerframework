# 说明

* 完全兼容MyBatis-Plus，在其基础上增加了加解密和字段hash功能。
* 分页插件已默认启用
* 对Druid数据源的参数进行了优化，简化配置，只需要配置（JdbcUrl、UserName、Password）三个即可，默认数据库是Mysql

# 使用方式

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

2. 引入 mybatis 模块

mybatis模块已经引入了mysql、druid，代码中共无需再引入它们

```xml
      <dependency>
         <groupId>com.bkjk.platform.summerframework</groupId>
         <artifactId>platform-starter-mybatis</artifactId>
      </dependency>
```

## 演示代码

* 数据源配置

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    platform: mysql
    url: jdbc:mysql://localhost:3306/test?useSSL=false&characterEncoding=utf8
    username: root
    password: root
```

* 实体类
```java
@TableName("city")
@Data
public class City implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type=AUTO)
    private Long id;

    private String name;

    @TableField("state")
    private EncryptedColumn state;

    @TableField("country")
    private String country;

    private Sha1Column hash;
 }
```
> 说明:如果表字段采用了例如：key, value, order作为字段,会报错org.springframework.jdbc.BadSqlGrammarException。此时需要处理关键字为‘关键字’，
   示例:@TableField(value="'key'")

* Mapper
```java
@Mapper
public interface CityMapper extends MyBaseMapper<City> {

    @Select("select count(0) from city")
    int test();
}

```

* Service

```java
@Service
public class CityService {
  
    @Autowired
    private CityMapper cityMapper;
  
    /***
     * @Description: 创建新城市。并返回插入数据的条数
     * @Param: [name]
     * @Return: int
     * @Author: shaoze.wang
     * @Date: 2019/2/14 16:54
     */
    public int createCity(String name) {
        City newCity = new City();
        newCity.setName(name);
        return cityMapper.insert(newCity);
    }


    /***
     * @Description: 创建新城市。返回city实体
     * @Param: [name]
     * @Return: boolean
     * @Author: shaoze.wang
     * @Date: 2019/2/14 16:55
     */
    public City createCity2(String name) {
        City newCity = new City();
        newCity.setName(name);
        if (cityMapper.insert(newCity)>0) {
            return newCity;
        } else {
            return null;
        }
    }

    /***
     * @Description: 根据id更新城市名称，返回更新成功的条数
     * @Param: [id, newName]
     * @Return: int
     * @Author: shaoze.wang
     * @Date: 2019/2/14 16:58
     */
    public int updateCity(long id, String newName) {
        City cityUpdate = new City();
        cityUpdate.setId(id);
        cityUpdate.setName(newName);
        return cityMapper.updateById(cityUpdate);
    }


    /***
     * @Description: 根据名称查询城市
     * @Param: [name]
     * @Return: com.bkjk.platform.mybatis.sample.City
     * @Author: shaoze.wang
     * @Date: 2019/2/14 17:13
     */
    public City findCityByName(String name) {
        return cityMapper.selectOne(Wrappers.<City>query().eq(getFieldName(City::getName), name));
    }

    /***
     * @Description: 根据名称模糊查询
     * @Param: [name]
     * @Return: java.util.List<com.bkjk.platform.mybatis.sample.City>
     * @Author: shaoze.wang
     * @Date: 2019/2/14 17:13
     */
    public List<City> findManyCityByNameLike(String name) {
        return cityMapper.selectList(Wrappers.<City>query().like(getFieldName(City::getName), name));
    }
    /***
     * @Description: 根据名称做分页排序查询，ascs是升序排列的字段。page=1 size=10 表示查询第一页，每页10条数据
     * @Param: [name, page, size, ascs]
     * @Return: com.baomidou.mybatisplus.extension.plugins.pagination.Page
     * @Author: shaoze.wang
     * @Date: 2019/2/20 14:14
     */
    public Page findPagedCityByNameAsc(String name, int page, int size, List<String> ascs) {
        Page pageRequest = new Page(page, size);
        pageRequest.setAscs(ascs);
        Map<String, Object> param=new HashMap<>();
        param.put(getFieldName(City::getName),name);
        return cityMapper.selectPageAccurate(pageRequest,param);
    }

    /***
     * @Description: 根据名称做分页排序查询(用like做模糊查询)，ascs是升序排列的字段。page=1 size=10 表示查询第一页，每页10条数据
     * @Param: [name, page, size, ascs]
     * @Return: com.baomidou.mybatisplus.extension.plugins.pagination.Page
     * @Author: shaoze.wang
     * @Date: 2019/2/14 17:14
     */
    public Page findPagedCityByNameLikeAsc(String name, int page, int size, List<String> ascs) {
        Page pageRequest = new Page(page, size);
        pageRequest.setAscs(ascs);
        Map<String, Object> param=new HashMap<>();
        param.put(getFieldName(City::getName),name);
        return cityMapper.selectPageBlurry(pageRequest,param);
    }



    /***
     * @Description: 根据ID删除
     * @Param: [id]
     * @Return: int
     * @Author: shaoze.wang
     * @Date: 2019/2/14 17:16
     */
    public int deleteById(long id) {
        return cityMapper.deleteById(id);
    }

    /***
     * @Description: 根据id批量删除
     * @Param: [ids]
     * @Return: int
     * @Author: shaoze.wang
     * @Date: 2019/2/14 17:16
     */
    public int deleteByIds(List<Long> ids) {
        return cityMapper.deleteBatchIds(ids);
    }

    /***
     * @Description: 根据名称删除 ，返回删除的条数
     * @Param: [name]
     * @Return: int
     * @Author: shaoze.wang
     * @Date: 2019/2/14 17:16
     */
    public int deleteByName(String name) {
        Map<String, Object> columnMap = new HashMap<>();
        columnMap.put(getFieldName(City::getName), name);
        return cityMapper.deleteByMap(columnMap);
    }

    /*** 
    * @Description: 根据name更新country，先查询出ID，然后根据ID进行更新
    * @Param: [name, country] 
    * @Return: boolean 
    * @Author: shaoze.wang
    * @Date: 2019/2/15 10:56 
    */
    public int updateCountryByName(String name, String country) {
        Map<String,Object> param=new HashMap<>();
        param.put(getFieldName(City::getName),name);
        return cityMapper.updateBatchById(cityMapper
                .selectByMap(param)
                .stream()
                .map(city -> {
                    City updateCity = new City();
                    updateCity.setId(city.getId());
                    updateCity.setCountry(country);
                    return updateCity;
                })
                .collect(Collectors.toList()));
    }

}
```

参考 [platform-starter-mybatis-sample](https://code.bkjk-inc.com/projects/SOA/repos/summerframework2/browse/summerframework-samples/platform-starter-mybatis-sample)

## 从1.X升级到2.X

版本1.X中是我们自己实现了SQL的拼接与执行，2.X中我们改用MyBatis-Plus。原因是对比了基于MyBatis的各种ORM之后，发现MyBatis-Plus的功能最为完善（批量、分页、枚举转换、逻辑删除、乐观锁等都有）不需要重造轮子了。

### 1.X与2.X的代码差异

#### 注解

在1.X中实体不需要注解，而2.X中实体必须有注解

* 类注解：@TableName("city")
* 字段注解：@TableField("state")
* 主键注解：@TableId(type=AUTO)

***注意：主键注解的type一定要写，AUTO表示自增主键***，如果希望使用自定义主键参考[Sequence主键](https://mp.baomidou.com/guide/sequence.html)

#### MyBaseMapper

* 再1.X中用BaseMapper，在2.X中用 MyBaseMapper （添加了一些方便使用的方法）
* 在1.X中，BaseMapper 的泛型包括实体和主键的类型，2.X中只包含实体
* 在1.X中，BaseMapper 里的方法参数中必须包含Class，2.X中不需要包含
* 在1.X中，分页的第一页的页码是0，2.X中第一页的页码是1。
* 在1.X中，查询条件是map，2.X中查询条件是map、实体对象或者Wrapper对象
* 在1.X中，提供了模糊查询的方法，2.X中需要用Wrapper来自己组装模糊查询的条件
* 2.X版本提供一个获取字段名的工具类，可以替换之前用`map.put("columnName",value)`的代码。`import static com.bkjk.platform.mybatis.util.MyBatisUtil.getFieldName;`

方法对照表

| 1.X | 2.X |
| ------ | ------ |
| insert | 通过MyBaseMapper兼容 |
| insertSelective | 通过MyBaseMapper兼容 |
| updateById | 通过MyBaseMapper兼容 |
| updateByIdSelective | 通过MyBaseMapper兼容 |
| deleteById | 通过MyBaseMapper兼容 |
| selectById | 通过MyBaseMapper兼容 |
| selectByParams | 通过MyBaseMapper兼容 |
| selectCount | selectPageAccurate |
| selectPage | selectPageAccurate |
| selectCountVague | selectPageBlurry  |
| seletePageVague | selectPageBlurry  |


# 详细功能示例代码和测试用例

## 批量更新

开发中经常遇到要更新多条数据的情况，这时可用下面两种方式。建议采用第一种，根据主键进行更新，虽然写起来麻烦，但是效率更高，也更安全。

第二种方式写起来简单，但是可能引起死锁。我们一般用MySql的InnoDB做存储引擎。假设name字段有索引，用name做条件更新时，先对锁索引，再锁主键；而根据主键更新时只锁主键。如果有两个事务分别用索引和主键对同一行做更新，并发高时，这两个事务会产生死锁。所以要么用主键更新，要么都用索引更新，而实际上我们不可能都所有地方都用索引更新，所以最好的解决办法时永远只用主键更新。

```java


    /*** 
    * @Description: 根据name更新country，先查询出ID，然后根据ID进行更新
    * @Param: [name, country] 
    * @Return: boolean 
    * @Author: shaoze.wang
    * @Date: 2019/2/15 10:56 
    */
    public int updateCountryByName(String name, String country) {
        Map<String,Object> param=new HashMap<>();
        param.put(getFieldName(City::getName),name);
        return cityMapper.updateBatchById(cityMapper
                .selectByMap(param)
                .stream()
                .map(city -> {
                    City updateCity = new City();
                    updateCity.setId(city.getId());
                    updateCity.setCountry(country);
                    return updateCity;
                })
                .collect(Collectors.toList()));
    }

```

## 字段加密与哈希

利用自定义类型来实现字段的自动加解密。

### 配置

1. mybatis.encrypt配置加解密用的密码和盐
2. mybatis.sha1-column配置字段hash用的盐

```yaml
platform.mybatis:
  encrypt:
    password: password
    salt: salt
  sha1-column.salt: app1
```

### 代码
通过在实体中自定义类型来实现，比如下面代码表示state字段存储到数据库时时加密的，hash字段存到数据库里的时一段SHA1算法做哈希之后的值

```java
    @TableField("state")
    private EncryptedColumn state; // 加密字段
    private Sha1Column hash; // hash字段
```

测试用例

```java
    @Test
    public void testEncrypted(){
        City city = new City();
        city.setName("成都");
        city.setState(EncryptedColumn.create("四川"));
        city.setCountry("中国");
        city.setHash(Sha1Column.create("abc"));
        service.save(city);
        System.out.println(cityMapper.selectById(city.getId()));

        Assert.assertEquals("四川",cityMapper.selectById(city.getId()).getState().getValue());
    }
```

`service.save(city);` 产生的SQL是
```text
INSERT INTO city  ( name, state, country, hash )  VALUES  ( '成都', '45ffd17bf74bad972a6bc45dc5c949fedacd773b5d4205f9888129d597fa6b47ae9c01054d4f', '中国', '119cd5e789ac6a7406c093e5bdd1d39b2e82db5a' )
```

`System.out.println(cityMapper.selectById(city.getId()));` 输出的结果是
```text
City{id=56, name='成都', state=EncryptedColumn{value='四川'}, country='中国', hash=Sha1Column{value='119cd5e789ac6a7406c093e5bdd1d39b2e82db5a'}}
```

可以看到存储 ("四川") 时已经被加密，hash字段也是哈希后的值，通过mapper查询出的实体中被解密成功（可通过city.getState().getValue()`获取明文）。


更多功能可参考[MyBatis-Plus 官网](https://mp.baomidou.com/guide/crud-interface.html)