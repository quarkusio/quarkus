package org.acme;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SomeBean {

    public String getValue() {
        return "value";
    }
}
