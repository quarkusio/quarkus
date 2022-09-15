package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyHelloExample {

    public String hello() {
        return "My Example Hello Quarkus Codestart";
    }

}
