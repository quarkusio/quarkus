package org.acme.testlib.mock;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import io.quarkus.arc.DefaultBean;
import org.acme.Service;

@Dependent
public class MockServiceProducer {

    @Produces
    public Service getMockService() {
        return new MockService();
    }
}