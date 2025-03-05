package org.acme;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.arc.InjectableBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(CallbackInvokingInterceptor.class)
public class QuarkusTestAccessingBeanInCallback {

    @Inject
    InjectableBean bean;

    // Crude validation that the callback happened; if we are run with different phases in different classloaders this will fail, but that's a problem anyway
    boolean callbackHappened = false;

    @Callback
    public void callback() {
        callbackHappened = true;
        // Callbacks invoked by test frameworks should be able to see injected beans
        assertNotNull(bean, "A method invoked by a test interceptor should have access to CDI beans");

    }

    // Checks that if a template isn't used, we can see the bean
    @Test
    public void beansShouldBeInjected() {
        // The main interest here is in the callback, but do a sense check of the injected bean
        assertNotNull(bean, "With no test framework involved, the injected bean should be present");
    }

    @Test
    public void callbackShouldBeInvoked() {
        assertTrue(callbackHappened, "Either the callback did not happen, or it happened in a different classloader");
    }

}
