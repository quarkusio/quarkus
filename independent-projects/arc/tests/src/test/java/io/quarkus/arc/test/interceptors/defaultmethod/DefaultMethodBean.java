package io.quarkus.arc.test.interceptors.defaultmethod;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ABinding
public class DefaultMethodBean implements DefaultMethodInterface {

    public String hello() {
        return "hello";
    }
}
