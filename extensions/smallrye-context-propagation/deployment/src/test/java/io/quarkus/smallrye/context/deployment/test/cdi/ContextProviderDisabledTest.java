package io.quarkus.smallrye.context.deployment.test.cdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.QuarkusUnitTest;

public class ContextProviderDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().overrideConfigKey("quarkus.arc.context-propagation.enabled",
            "false");

    @Inject
    ManagedExecutor all;

    @Inject
    MyRequestBean bean;

    @Test
    public void testPropagationDisabled() throws InterruptedException, ExecutionException, TimeoutException {
        ManagedContext requestContext = Arc.container().requestContext();

        requestContext.activate();
        assertEquals("FOO", bean.getId());
        try {
            assertEquals("OK",
                    all.completedFuture("OK").thenApplyAsync(text -> {
                        // Assertion error would result in an ExecutionException thrown from the CompletableFuture.get()
                        assertFalse(requestContext.isActive());
                        try {
                            bean.getId();
                            fail();
                        } catch (ContextNotActiveException expected) {
                        }
                        return text;
                    }).toCompletableFuture().get(5, TimeUnit.SECONDS));
        } finally {
            requestContext.terminate();
        }
    }

    @RequestScoped
    public static class MyRequestBean {

        String id;

        @PostConstruct
        void init() {
            id = "FOO";
        }

        public String getId() {
            return id;
        }

    }

}
