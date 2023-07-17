package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LibraryBean {

    private final CommonTransitiveBean bean;

    public LibraryBean(CommonTransitiveBean bean) {
        this.bean = java.util.Objects.requireNonNull(bean);
    }
}
