package io.quarkus.arc.test.unproxyable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.unproxyable.some.Resource;
import io.quarkus.test.QuarkusUnitTest;

// This test aims to test the https://github.com/quarkusio/quarkus/issues/22815 in Quarkus integration
// There is a duplicate test for ArC standalone: io.quarkus.arc.test.clientproxy.constructor.ProducerReturnTypePackagePrivateNoArgsConstructorTest
public class ProducerReturnTypePackagePrivateNoArgsConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(ProducerReturnTypePackagePrivateNoArgsConstructorTest.class, ResourceProducer.class,
                            Resource.class));

    @Inject
    Instance<Resource> instance;

    @Test
    public void testProducer() throws IOException {
        assertTrue(instance.isResolvable());
        assertEquals(5, instance.get().ping());
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
