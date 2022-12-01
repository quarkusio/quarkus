package io.quarkus.spring.security.deployment.springapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfiguration {

    @Bean
    public PersonChecker personChecker() {
        return new PersonCheckerImpl();
    }
}
