package io.quarkus.rest.test.simple;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class HelloService {

    public String sayHello() {
        return "Hello";
    }
}
