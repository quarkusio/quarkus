package org.acme;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class LibraryTestBean implements LibraryBeanInterface {

    @Inject LibraryTestDepBean depBean;

    @Override
    public String getValue() {
        return depBean.getValue();
    }
}
