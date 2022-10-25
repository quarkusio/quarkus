package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LibraryTestBean implements LibraryBeanInterface {

    @Inject LibraryTestDepBean depBean;

    @Override
    public String getValue() {
        return depBean.getValue();
    }
}
