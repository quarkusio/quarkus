package io.quarkus.arc.test.interceptors.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AsyncContinuationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class,
            AlphaInterceptor.class, BravoInterceptor.class, CharlieInterceptor.class);

    private static ExecutorService executor;

    @BeforeAll
    static void init() {
        executor = Executors.newFixedThreadPool(1);
    }

    @AfterAll
    static void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testAsyncExecution() throws IOException, InterruptedException {
        BravoInterceptor.reset();
        assertEquals("A1:dummy:A2", Arc.container().instance(SimpleBean.class).get().foo());
        assertTrue(BravoInterceptor.latch.await(3, TimeUnit.SECONDS));
        assertEquals("C1:ok:C2", BravoInterceptor.asyncResult);
    }

    @Simple
    @Singleton
    static class SimpleBean {

        String foo() {
            return "ok";
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class AlphaInterceptor {

        @AroundInvoke
        Object around(InvocationContext ctx) throws Exception {
            return "A1:" + ctx.proceed() + ":A2";
        }
    }

    @Simple
    @Priority(2)
    @Interceptor
    public static class BravoInterceptor {

        static CountDownLatch latch;
        static String asyncResult;

        static void reset() {
            latch = new CountDownLatch(1);
            asyncResult = null;
        }

        @AroundInvoke
        Object around(InvocationContext ctx) throws Exception {
            executor.submit(() -> {
                try {
                    asyncResult = ctx.proceed().toString();
                    latch.countDown();
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            });
            return "dummy";
        }
    }

    @Simple
    @Priority(3)
    @Interceptor
    public static class CharlieInterceptor {

        @AroundInvoke
        Object around(InvocationContext ctx) throws Exception {
            return "C1:" + ctx.proceed() + ":C2";
        }
    }

}
