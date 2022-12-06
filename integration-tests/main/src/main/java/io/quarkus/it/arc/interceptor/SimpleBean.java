package io.quarkus.it.arc.interceptor;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimpleBean {

    @Simple(name = "foo")
    @Simple(name = "bar")
    public String ping() {
        return "OK";
    }

}
