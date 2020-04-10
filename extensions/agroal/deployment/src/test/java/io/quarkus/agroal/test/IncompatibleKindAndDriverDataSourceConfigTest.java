package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class IncompatibleKindAndDriverDataSourceConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("incompatible-kind-and-driver-datasource.properties")
            .setExpectedException(RuntimeException.class);

    @Test
    public void testApplicationShouldNotStart() {
        fail("Application should not start when db-kind and driver are incompatible");
    }
}
