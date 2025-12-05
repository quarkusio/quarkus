package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Identifier;

public class ActiveInactiveClientProgrammaticAccessOnlyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("active-inactive-programmatic.properties", "application.properties"));

    @Test
    void smoke() {
        assertTrue(Arc.container().select(RestClient.class).getHandle().getBean().isActive());
        assertFalse(
                Arc.container().select(RestClient.class, Identifier.Literal.of("client2")).getHandle().getBean().isActive());
        assertTrue(Arc.container().select(RestClient.class, Identifier.Literal.of("client3")).getHandle().getBean().isActive());
    }
}
