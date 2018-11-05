package org.jboss.protean.arc;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class GeneratedStringProducer {

    @Dependent
    @Generated
    @Produces
    String produce() {
        return "" + System.nanoTime();
    }

}
