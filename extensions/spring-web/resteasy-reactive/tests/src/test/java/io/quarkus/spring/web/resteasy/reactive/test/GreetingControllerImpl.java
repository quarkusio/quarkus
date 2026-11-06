package io.quarkus.spring.web.resteasy.reactive.test;

import org.springframework.stereotype.Component;

@Component
public class GreetingControllerImpl implements GreetingControllerInterface {

    @Override
    public String hello() {
        return "hello from interface";
    }
}
