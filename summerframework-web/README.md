# 说明

* 将monitor，configcenter，webapi，eureka，openfeign等微服务的组件传递引入
* 服务治理埋点

# 使用

* 应用配置

```
info:
   groupId: @project.groupId@
   artifactId: @project.artifactId@
   version: @project.version@
```

* 依赖配置

```
<plugin>
    <groupId>pl.project13.maven</groupId>
    <artifactId>git-commit-id-plugin</artifactId>
    <version>2.1.15</version>
    <executions>
            <execution>
                    <goals>
                        <goal>revision</goal>
                    </goals>
            </execution>
    </executions>
    <configuration>
            <dotGitDirectory>${project.basedir}/.git</dotGitDirectory>
    </configuration>
</plugin>

```


# 注意

* 如果项目不使用spring security，可以用如下方式排除它的配置


```java
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
```
