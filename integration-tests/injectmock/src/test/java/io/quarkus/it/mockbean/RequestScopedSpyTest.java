package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

/**
 * Tests that Mockito spies can be RequestScoped - this case is different from app scoped/singletons because by the time
 * we create spies (right after creating test instance), we need to manually activate req. context for this creation.
 */
@QuarkusTest
public class RequestScopedSpyTest {

    @InjectSpy
    private RequestBean spiedBean;

    @Inject
    private SomeOtherBean injectedBean;

    @Test
    void verifySpyWorks() {
        // Executes gracefully
        assertNotNull(spiedBean);
        injectedBean.pong();
        Mockito.verify(spiedBean, Mockito.times(1)).ping();
    }

    @Nested
    class NestedTest {
        @Test
        void verifyNestedSpyWorks() {
            assertNotNull(spiedBean);
            injectedBean.pong();
            Mockito.verify(spiedBean, Mockito.times(1)).ping();
        }
    }

    @RequestScoped
    public static class RequestBean {

        public void ping() {

        }
    }

    @ApplicationScoped
    public static class SomeOtherBean {

        @Inject
        RequestBean bean;

        public void pong() {
            bean.ping();
        }
    }
}
