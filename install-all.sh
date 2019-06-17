#!/usr/bin/env bash


cd summerframework-common        && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-configcenter  && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mybatis       && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-webapi        && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-eureka        && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-openfeign     && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-redis         && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-rabbit        && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-monitor       && mvn install -U -DskipTests -Dmaven.javadoc.skip=true && cd ..