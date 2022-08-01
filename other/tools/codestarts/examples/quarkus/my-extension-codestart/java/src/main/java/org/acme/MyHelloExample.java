package org.acme;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyHelloExample {

    public String hello() {
        return "My Example Hello Quarkus Codestart";
    }

}
