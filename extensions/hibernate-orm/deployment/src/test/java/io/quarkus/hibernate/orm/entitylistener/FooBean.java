package io.quarkus.hibernate.orm.entitylistener;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class FooBean {

    public String pleaseDoNotCrash() {
        return "Yeah!";
    }
}
