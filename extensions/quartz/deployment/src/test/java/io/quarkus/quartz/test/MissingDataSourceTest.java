package io.quarkus.quartz.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.test.QuarkusUnitTest;

public class MissingDataSourceTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(ConfigurationError.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleJobs.class)
                    .addAsResource(new StringAsset("quarkus.quartz.store-type=jdbc-cmt"), "application.properties"));

    @Test
    public void shouldFailAndNotReachHere() {
        Assertions.fail();
    }
}
