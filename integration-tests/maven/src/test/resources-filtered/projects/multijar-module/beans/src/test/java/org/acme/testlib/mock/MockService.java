package org.acme.testlib.mock;

import org.acme.Service;

public class MockService implements Service {

    public String getId() {
        return "mock-service";
    }
}