package org.acme;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LibraryBean implements LibraryBeanInterface {

    @Override
    public String getValue() {
        return "main";
    }
}
