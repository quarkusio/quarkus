package org.acme;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.acme.services.Registration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClientCallingResourceTest {

    @Inject
    Registration registration;

    @Test
    public void test() {
        Assertions.assertNotNull(registration);
    }

}
