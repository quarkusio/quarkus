package io.quarkus.it.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleApplication {

    @Bean
    @ConfigurationProperties
    public BeanProperties beanProperties() {
        BeanProperties result = new BeanProperties("final");
        result.packagePrivateValue = 100;
        return result;
    }
}
