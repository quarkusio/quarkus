package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LibraryTestDepBean {

    public String getValue() {
        return "test";
    }
}
