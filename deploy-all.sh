#!/usr/bin/env bash


cd summerframework-common        && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-configcenter  && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-mybatis       && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-webapi        && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-eureka        && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-openfeign     && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..
cd summerframework-monitor       && mvn clean deploy -DskipTests -Dmaven.javadoc.skip=true && cd ..