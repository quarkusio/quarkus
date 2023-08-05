package io.quarkus.it.flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisplayName("Tests flyway initialization")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Our Windows CI does not have Docker installed properly")
public class FlywayInitializationTest {

    @Test
    public void testInitialization() {

    }

}
