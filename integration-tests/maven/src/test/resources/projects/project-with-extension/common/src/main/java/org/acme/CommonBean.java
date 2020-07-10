package org.acme;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CommonBean {

    private final CommonTransitiveBean bean;

    public CommonBean(CommonTransitiveBean bean) {
        this.bean = java.util.Objects.requireNonNull(bean);
    }
}
