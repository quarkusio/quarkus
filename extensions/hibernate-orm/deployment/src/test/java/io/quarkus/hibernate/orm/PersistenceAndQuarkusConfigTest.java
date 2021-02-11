package io.quarkus.hibernate.orm;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class PersistenceAndQuarkusConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(ConfigurationError.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyEntity.class)
                    .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml")
                    .addAsResource("application.properties"));

    @Test
    public void testPersistenceAndConfigTest() {
        // should not be called, deployment exception should happen first:
        // it's illegal to have Hibernate configuration properties in both the
        // application.properties and in the persistence.xml
        Assertions.fail();
    }

}
