package io.quarkus.it.spring;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
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

    @Bean
    @CustomPrototype
    public CustomPrototypeBean beanWithCustomPrototype() {
        return new CustomPrototypeBean();
    }

    private AtomicInteger prototypeBeanCounter = new AtomicInteger(0);

    @Bean
    @Scope(scopeName = "prototype")
    public PrototypeBean prototypeBean() {
        return new PrototypeBean(prototypeBeanCounter.getAndIncrement());
    }

    private static class SingletonBean {

    }

    private static class AnotherRequestBean {

    }

    public static class PrototypeBean {

        public final int index;

        public PrototypeBean(int index) {
            this.index = index;
        }
    }

    public static class CustomPrototypeBean {

    }

    @Named
    public static class NamedBean {

    }
}
