package org.acme;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import io.quarkus.arc.DefaultBean;

@Dependent
public class AcmeServiceProducer {

    @Produces
    @DefaultBean
    public Service getAcmeService() {
        return new AcmeService();
    }
}
