package io.quarkus.quartz.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class UnsupportedClusteredJobConfigurationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setExpectedException(ConfigurationException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleJobs.class)
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.store-type=ram\nquarkus.quartz.clustered=true"),
                            "application.properties"));

    @Test
    public void shouldFailWhenConfiguringClusteredJobWithRamStore() {
        Assertions.fail();
    }
}
