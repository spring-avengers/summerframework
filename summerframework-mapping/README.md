mapping starter引入了基于字节码动态生成的高性能Bean Mapping框架Orika，并且添加了一些小功能方便使用：

- 基于表达式强类型的配置
- 枚举和Java 8日期类型的转换器
- 通过配置文件进行配置

有关Orika，详见 http://orika-mapper.github.io/orika-docs/，经测试发现，Orika的性能是Spring BeanUtils.copyProperties的4倍左右，而且功能强大很多。