package org.shamrock.jpa.tests.configurationless;

import org.jboss.shamrock.deployment.configuration.ConfigurationError;
import org.jboss.shamrock.test.BuildShouldFailWith;
import org.jboss.shamrock.test.Deployment;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(ShamrockUnitTest.class)
public class PersistenceAndShamrockConfigTest {
    @Deployment
    @BuildShouldFailWith(ConfigurationError.class)
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(Gift.class, CRUDResource.class, ConfigurationlessApp.class)
                .addAsManifestResource("META-INF/some-persistence.xml", "persistence.xml")
                .addAsManifestResource("META-INF/microprofile-config.properties");
    }

    @Test
    public void testPersistenceAndConfigTest() {
        // should not be called, deployment exception should happen first.
        fail();
    }

}
