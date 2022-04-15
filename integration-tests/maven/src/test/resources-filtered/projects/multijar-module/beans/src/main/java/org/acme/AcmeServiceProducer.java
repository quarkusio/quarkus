package org.acme;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import io.quarkus.arc.DefaultBean;

@Dependent
public class AcmeServiceProducer {

    @Produces
    @DefaultBean
    public Service getAcmeService() {
        return new AcmeService();
    }
}