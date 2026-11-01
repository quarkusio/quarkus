package io.quarkus.hibernate.orm.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
            //.setExpectedException(ConfigurationException.class)
            .assertException(
                    throwable -> assertTrue(throwable.getMessage().contains(
                            "the deprecated 'ddl' validation mode conflicts with 'ddl-influence=required'. " +
                                    "Remove 'ddl' from 'quarkus.hibernate-orm.validation.mode' and use " +
                                    "'quarkus.hibernate-orm.validation.ddl-influence' instead")))
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "callback,ddl")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "required");

    @Test
    public void testDeploymentShouldFail() {
        Assertions.fail();
    }
}
