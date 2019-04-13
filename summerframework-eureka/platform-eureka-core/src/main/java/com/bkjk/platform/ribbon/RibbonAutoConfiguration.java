
package com.bkjk.platform.ribbon;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@RibbonClients(defaultConfiguration = {RibbonCientConfiguration.class})
public class RibbonAutoConfiguration {

}
