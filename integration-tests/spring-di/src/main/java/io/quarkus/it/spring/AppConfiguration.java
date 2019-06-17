package io.quarkus.it.spring;

import javax.inject.Singleton;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class AppConfiguration {

    @Bean(name = "cap")
    public StringFunction capitalizer(@Qualifier("dumb") Dummy notUsedJustMakingSureDIInMethodsWorks,
            OtherDummy alsoNotUsed) {
        return String::toUpperCase;
    }

    @Bean
    @Singleton
    public SingletonBean explicitSingletonBean() {
        return new SingletonBean();
    }

    @Bean
    public SingletonBean implicitSingletonBean() {
        return new SingletonBean();
    }

    @Bean
    @RequestScope
    public AnotherRequestBean requestBean() {
        return new AnotherRequestBean();
    }

    private static class SingletonBean {

    }

    private static class AnotherRequestBean {

    }
}
