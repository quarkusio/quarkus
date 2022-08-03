package io.quarkus.context.test;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public static volatile int DESTROY_INVOKED = 0;

    public String callMe() {
        return "Hello " + System.identityHashCode(this);
    }

    @PreDestroy
    public void destroy() {
        DESTROY_INVOKED++;
    }
}
