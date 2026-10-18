package org.acme.shared;

import jakarta.enterprise.inject.Produces;

public class BigBeanProducer {

    @Produces
    public BigBean getBigBean() {
        return new BigBean();
    }
}
