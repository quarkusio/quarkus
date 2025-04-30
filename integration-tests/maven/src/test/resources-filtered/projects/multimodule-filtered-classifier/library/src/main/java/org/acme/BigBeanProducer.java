package org.acme;

import jakarta.enterprise.inject.Produces;
import org.acme.shared.BigBean;

/**
 * The purpose of this class is to create a conflict with the shared BigBeanProducer
 */
public class BigBeanProducer {

    @Produces
    public BigBean getName() {
        return new BigBean();
    }
}
