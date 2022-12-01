package org.acme;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LibraryTestDepBean {

    public String getValue() {
        return "test";
    }
}
