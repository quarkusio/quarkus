package io.quarkus.it.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

public class SampleApplication {

    @ConfigurationProperties
    public BeanProperties beanProperties() {
        return new BeanProperties();
    }
}
