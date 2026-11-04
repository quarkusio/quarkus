package org.acme.testlib.mock;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import org.acme.Service;

@Dependent
public class MockServiceProducer {

    @Produces
    public Service getMockService() {
        return new MockService();
    }
}
