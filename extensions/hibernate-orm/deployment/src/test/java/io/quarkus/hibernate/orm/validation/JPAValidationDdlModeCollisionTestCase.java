package io.quarkus.hibernate.orm.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that using the deprecated 'ddl' validation mode alongside an 'auto' or 'required'
 * ddl-influence value throws a {@link ConfigurationException}.
 */
public class JPAValidationDdlModeCollisionTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .setExpectedException(ConfigurationException.class)
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "callback,ddl")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "required");

    @Test
    public void testDeploymentShouldFail() {
        Assertions.fail();
    }
}
