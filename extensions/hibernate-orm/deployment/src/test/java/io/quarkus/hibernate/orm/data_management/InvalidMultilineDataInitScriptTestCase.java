package io.quarkus.hibernate.orm.data_management;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidMultilineDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .setExpectedException(PersistenceException.class)
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-invalid-multiline-data-init-script-test.properties", "application.properties")
                    .addAsResource("invalid-multiline.sql")
                    .addClasses(MyEntity.class));

    @Test
    public void testInvalidMultilineDataInitScript() {
        // should not be called, deployment exception should happen first.
        // A multiline sql file with an sql statement not terminated by a semicolon should fail.
        Assertions.fail();
    }
}
