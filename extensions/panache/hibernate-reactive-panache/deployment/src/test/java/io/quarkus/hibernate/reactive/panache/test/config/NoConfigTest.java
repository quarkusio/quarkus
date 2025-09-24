package io.quarkus.hibernate.reactive.panache.test.config;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addAsResource("application-datasource-only.properties", "application.properties"));

    @Test
    public void testNoConfig() {
        // we should be able to start the application, even with no (Hibernate/Panache) configuration at all
    }
}
