package io.quarkus.arc.test.clientproxy.constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.clientproxy.constructor.some.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// This test aims to test the https://github.com/quarkusio/quarkus/issues/22815 in ArC standalone
// There is a duplicate test for Quarkus integration: io.quarkus.arc.test.unproxyable.ProducerReturnTypePackagePrivateNoArgsConstructorTest
public class ProducerReturnTypePackagePrivateNoArgsConstructorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ResourceProducer.class);

    @Test
    public void testProducer() throws IOException {
        Resource res = Arc.container().instance(Resource.class).get();
        assertNotNull(res);
        assertTrue(res instanceof ClientProxy);
        assertEquals(5, res.ping());
    }

    @Singleton
    static class ResourceProducer {

        @ApplicationScoped
        @Produces
        Resource resource() {
            return Resource.from(5);
        }

    }

}
