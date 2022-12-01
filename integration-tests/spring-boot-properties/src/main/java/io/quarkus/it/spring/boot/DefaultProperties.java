package io.quarkus.it.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class DefaultProperties {

    private String value = "default-value";

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
