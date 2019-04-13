@echo off



cmd /c "cd summerframework-common        && mvn deploy -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-configcenter  && mvn deploy -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-mybatis       && mvn deploy -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-webapi        && mvn deploy -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-eureka        && mvn deploy -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-openfeign     && mvn deploy -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-monitor       && mvn deploy -DskipTests -Dmaven.javadoc.skip=true && cd .. "

pause