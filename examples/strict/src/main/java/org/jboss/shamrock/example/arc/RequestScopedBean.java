package org.jboss.shamrock.example.arc;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedBean {

    int count;

    public int incrementAndGet() {
        return ++count;
    }

}
