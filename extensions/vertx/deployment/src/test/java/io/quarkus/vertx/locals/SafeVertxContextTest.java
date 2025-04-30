package io.quarkus.vertx.locals;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.SafeVertxContext;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.mutiny.core.Vertx;

/**
 * Verify the behavior of the interceptor handling {@link SafeVertxContext}
 */
public class SafeVertxContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(MyBean.class));

    @Inject
    MyBean bean;

    @Inject
    Vertx vertx;

    @Test
    void testWhenRunningFromUnmarkedDuplicatedContext() throws InterruptedException {
        Context dc = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        CountDownLatch latch = new CountDownLatch(1);
        dc.runOnContext(ignored -> {
            bean.run();
            bean.runWithForce();
            latch.countDown();
        });

        Assertions.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testWhenRunningFromSafeDuplicatedContext() throws InterruptedException {
        Context dc = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        VertxContextSafetyToggle.setContextSafe(dc, true);
        CountDownLatch latch = new CountDownLatch(1);
        dc.runOnContext(ignored -> {
            bean.run();
            bean.runWithForce();
            latch.countDown();
        });

        Assertions.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testWhenRunningFromUnsafeDuplicatedContext() throws InterruptedException {
        Context dc = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        VertxContextSafetyToggle.setContextSafe(dc, false);
        CountDownLatch latch = new CountDownLatch(1);
        dc.runOnContext(ignored -> {
            try {
                bean.run();
                fail("The interceptor should have failed.");
            } catch (IllegalStateException e) {
                // Expected
            }
            bean.runWithForce();
            latch.countDown();
        });

        Assertions.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testWhenRunningOnARootContext() throws InterruptedException {
        Context dc = vertx.getDelegate().getOrCreateContext();
        CountDownLatch latch = new CountDownLatch(1);
        dc.runOnContext(ignored -> {
            try {
                bean.run();
                fail("The interceptor should have failed.");
            } catch (IllegalStateException e) {
                // Expected
            }
            try {
                bean.run();
                fail("The interceptor should have failed.");
            } catch (IllegalStateException e) {
                // Expected
            }
            latch.countDown();
        });

        Assertions.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testWhenRunningWithoutAContext() {
        try {
            bean.run();
            fail("The interceptor should have failed.");
        } catch (IllegalStateException e) {
            // Expected
        }
        try {
            bean.run();
            fail("The interceptor should have failed.");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @ApplicationScoped
    public static class MyBean {

        @SafeVertxContext
        public void run() {
            assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("ErrorVeto", "ErrorDoubt");
        }

        @SafeVertxContext(force = true)
        public void runWithForce() {
            assertTrue(VertxContext.isOnDuplicatedContext());
            VertxContextSafetyToggle.validateContextIfExists("ErrorVeto", "ErrorDoubt");
        }

    }

}
