#!/usr/bin/env bash


cd summerframework-common        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-configcenter  && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mybatis       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-webapi        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-eureka        && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-openfeign     && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-monitor       && mvn install -DskipTests -Dmaven.javadoc.skip=true && cd ..