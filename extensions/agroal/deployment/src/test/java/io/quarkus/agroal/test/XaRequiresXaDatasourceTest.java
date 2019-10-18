package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class XaRequiresXaDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("base.properties")
            .overrideConfigKey("quarkus.datasource.transactions", "XA")
            .assertException(t -> {
                assertEquals(DeploymentException.class, t.getClass());
            });

    @Test
    public void xaRequiresJta() {
        //Should not be reached: verify
        assertTrue(false);
    }

}
