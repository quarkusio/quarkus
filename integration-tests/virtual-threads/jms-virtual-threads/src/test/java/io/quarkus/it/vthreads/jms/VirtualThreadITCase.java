package io.quarkus.it.vthreads.jms;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@Disabled("Depends on Quarkiverse which causes a circular dependency")
public class VirtualThreadITCase extends VirtualThreadTest {
}
