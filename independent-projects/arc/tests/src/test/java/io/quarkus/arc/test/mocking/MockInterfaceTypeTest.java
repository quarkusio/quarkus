package io.quarkus.arc.test.mocking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.Mockable;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Reproduces: installing a mock for an interface type when the bean is a concrete implementation fails with
 * ClassCastException because the generated client proxy types the mock field to the concrete class,
 * not to the interface.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/53051">GitHub issue #53051</a>
 */
public class MockInterfaceTypeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyInterface.class, ProdImpl.class, ConsumerBean.class)
            .testMode(true)
            .build();

    @Test
    public void testMockByInterfaceType() {
        // Get the proxy for the interface type
        MyInterface proxy = Arc.container().select(MyInterface.class).get();
        assertInstanceOf(Mockable.class, proxy);

        // Install a mock implementation — this should NOT throw ClassCastException
        Mockable mockable = (Mockable) proxy;
        mockable.arc$setMock(new TestImpl());

        // Verify the mock is used
        assertEquals("mock", proxy.hello());

        // Clear the mock — original impl should be restored
        mockable.arc$clearMock();
        assertEquals("prod", proxy.hello());
    }

    interface MyInterface {
        String hello();
    }

    @ApplicationScoped
    static class ProdImpl implements MyInterface {
        @Override
        public String hello() {
            return "prod";
        }
    }

    static class TestImpl implements MyInterface {
        @Override
        public String hello() {
            return "mock";
        }
    }

    @ApplicationScoped
    static class ConsumerBean {
        // Inject by interface to ensure a client proxy is generated for the interface
        @Inject
        MyInterface iface;
    }
}
