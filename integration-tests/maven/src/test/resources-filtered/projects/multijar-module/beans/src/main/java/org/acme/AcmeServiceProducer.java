package org.acme;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Reserve;

@Dependent
public class AcmeServiceProducer {

    @Produces
    @Reserve
    @Priority(0)
    public Service getAcmeService() {
        return new AcmeService();
    }
}
