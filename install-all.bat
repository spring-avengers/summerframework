@echo off



cmd /c "cd summerframework-common        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-configcenter  && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-mybatis       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-webapi        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-eureka        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-openfeign     && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd .. "
cmd /c "cd summerframework-monitor       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd .. "

pause