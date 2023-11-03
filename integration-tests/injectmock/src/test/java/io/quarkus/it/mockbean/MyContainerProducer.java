package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;

@Singleton
public class MyContainerProducer {

    @ApplicationScoped
    @Produces
    public MyContainer<String> stringContainer() {
        return new MyContainer<>() {
            @Override
            public String getValue() {
                return "hello";
            }
        };
    }

    @ApplicationScoped
    @Produces
    public MyContainer<Integer> integerContainer() {
        return new MyContainer<>() {
            @Override
            public Integer getValue() {
                return 1;
            }
        };
    }
}
