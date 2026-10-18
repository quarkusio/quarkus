package io.quarkus.arc.test.lock;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Lock;
import io.quarkus.arc.impl.LockInterceptor;
import io.quarkus.arc.test.ArcTestContainer;

public class LockInterceptorDoubleWriteDeadlockTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(LockBean.class, LockInterceptor.class,
            LockInterceptor.class);

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    @Test
    void testLocks() throws ExecutionException, InterruptedException {
        LockBean lockBean = Arc.container().instance(LockBean.class).get();
        final var futures = new ArrayList<Future<String>>();
        for (int i = 0; i < 10; i++) {
            final Future<String> submit = threadPool.submit(() -> {
                lockBean.method1();
                return "value";
            });
            futures.add(submit);
        }

        for (final var future : futures) {
            future.get();
        }

    }

    @ApplicationScoped
    @Lock
    public static class LockBean {

        public void method1() {
            // invokes another intercepted write-locked method
            method2();
        }

        public void method2() {
        }
    }

}
