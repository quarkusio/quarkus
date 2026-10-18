package io.quarkus.flyway.test;

import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class FlywayExtensionValidateAtStartTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("validate-at-start-config.properties", "application.properties"))
            .setExpectedException(FlywayValidateException.class);

    @Test
    public void shouldNeverBeCalled() {

    }

}
