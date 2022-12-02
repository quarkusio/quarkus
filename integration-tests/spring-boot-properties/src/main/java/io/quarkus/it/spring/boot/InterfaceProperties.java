package io.quarkus.it.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public interface InterfaceProperties {

    String getValue();
}
