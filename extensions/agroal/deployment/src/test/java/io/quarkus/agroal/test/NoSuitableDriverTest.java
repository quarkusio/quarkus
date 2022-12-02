package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class NoSuitableDriverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-no-suitable-driver.properties")
            .assertException(t -> {
                assertEquals(ConfigurationException.class, t.getClass());
                assertThat(t).hasMessageStartingWith("Unable to find a JDBC driver corresponding to the database kind");
            });

    @Test
    public void noSuitableDriver() {
        // Should not be reached: verify
        assertTrue(false);
    }

}
