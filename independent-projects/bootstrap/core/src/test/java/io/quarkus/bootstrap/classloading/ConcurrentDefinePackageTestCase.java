package io.quarkus.bootstrap.classloading;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class ConcurrentDefinePackageTestCase {

    /**
     * Validates that {@link QuarkusClassLoader} can safely do concurrent class loading against "new" packages,
     * backed by {@link ClassLoader#definePackage(String, String, String, String, String, String, String, URL)}, which throws an
     * {@link IllegalArgumentException} if the package is already defined.
     */
    @Test
    public void concurrentDefinePackage() throws Exception {
        var threads = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
        var pool = Executors.newFixedThreadPool(threads);
        try (var quarkusClassLoader = QuarkusClassLoader.builder("test", Thread.currentThread().getContextClassLoader(), false)
                .build()) {
            for (var pkg = 0; pkg < 200; pkg++) {
                var readyLatch = new CountDownLatch(threads);
                var startLatch = new CountDownLatch(1);
                var pkgName = "my.package" + pkg;
                var futures = IntStream.range(0, threads).mapToObj(i -> CompletableFuture.runAsync(() -> {
                    readyLatch.countDown();
                    try {
                        assertThat(startLatch.await(5, TimeUnit.MINUTES)).isTrue();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    quarkusClassLoader.definePackage(pkgName, ClassPathElement.EMPTY);
                }, pool)).toArray(CompletableFuture[]::new);
                assertThat(readyLatch.await(5, TimeUnit.MINUTES)).isTrue();
                startLatch.countDown();
                CompletableFuture.allOf(futures).get(5, TimeUnit.MINUTES);
            }
        } finally {
            pool.shutdown();
        }
    }
}
