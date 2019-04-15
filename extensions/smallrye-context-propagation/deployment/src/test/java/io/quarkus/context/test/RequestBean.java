package io.quarkus.context.test;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {
    
    public String callMe() {
        return "Hello "+System.identityHashCode(this);
    }
}
