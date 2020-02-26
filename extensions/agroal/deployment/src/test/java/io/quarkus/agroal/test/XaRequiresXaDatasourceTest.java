package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class XaRequiresXaDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("base.properties")
            .overrideConfigKey("quarkus.datasource.jdbc.driver", "org.h2.Driver")
            .overrideConfigKey("quarkus.datasource.jdbc.transactions", "XA")
            .assertException(t -> {
                assertEquals(ConfigurationException.class, t.getClass());
            });

    @Test
    public void xaRequiresJta() {
        //Should not be reached: verify
        assertTrue(false);
    }

}
