package io.quarkus.resteasy.reactive.server.test.simple;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class HelloService {

    public String sayHello() {
        return "Hello";
    }
}
