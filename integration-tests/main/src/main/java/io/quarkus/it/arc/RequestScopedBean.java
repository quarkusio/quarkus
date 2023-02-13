package io.quarkus.it.arc;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.it.arc.somepackage.Superclass;

@RequestScoped
public class RequestScopedBean extends Superclass {

    int count;

    public int incrementAndGet() {
        return ++count;
    }

}
