package org.jboss.shamrock.example.arc;

import javax.enterprise.context.RequestScoped;

import org.jboss.shamrock.example.arc.somepackage.Superclass;

@RequestScoped
public class RequestScopedBean extends Superclass {

    int count;

    public int incrementAndGet() {
        return ++count;
    }

}
