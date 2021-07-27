package io.quarkus.it.mockbean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import javax.ws.rs.Produces;

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
