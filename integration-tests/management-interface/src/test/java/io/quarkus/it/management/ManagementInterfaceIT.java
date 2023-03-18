package io.quarkus.it.management;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class ManagementInterfaceIT extends ManagementInterfaceTestCase {

    @Override
    protected String getPrefix() {
        return "http://localhost:9000"; // ITs run in prod mode.
    }
}
