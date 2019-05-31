package io.quarkus.it.context;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public String callMe() {
        return "Hello " + System.identityHashCode(this);
    }
}
