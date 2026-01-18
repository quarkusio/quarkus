package io.quarkus.jfr.it;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.test.junit.QuarkusTest;
import jdk.jfr.Configuration;
import jdk.jfr.consumer.RecordingStream;

@ActivateRequestContext
@QuarkusTest
public class InfinispanJfrTest {

    private static final String DEFAULT_CLUSTER = "___DEFAULT-CLUSTER___";
    private static final String DEFAULT_CACHE_NAME = "test";
    private static final int TIMEOUT_SECONDS = 2;

    @Remote(DEFAULT_CACHE_NAME)
    @Inject
    RemoteCache<String, String> remoteCache;

    @Test
    void testAsyncSingle() throws IOException, ParseException, InterruptedException {
        Configuration config = Configuration.create(Paths.get("./noEvent.jfc"));
        try (RecordingStream stream = new RecordingStream(config)) {
            CountDownLatch latch = new CountDownLatch(3);
            stream.enable("quarkus.InfinispanSingleEntry");
            stream.enable("quarkus.InfinispanSingleEntryStart");
            stream.enable("quarkus.InfinispanSingleEntryEnd");
            stream.onEvent(e -> {
                latch.countDown();
                Assertions.assertNotNull(e.getString("traceId"));
                Assertions.assertEquals(DEFAULT_CACHE_NAME, e.getString("cacheName"));
                Assertions.assertEquals(DEFAULT_CLUSTER, e.getString("clusterName"));
                Assertions.assertEquals("putAsync", e.getString("method"));
            });
            stream.startAsync();

            remoteCache.putAsync("key", "value");

            Assertions.assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    void testAsyncAll() throws IOException, ParseException, InterruptedException {
        Configuration config = Configuration.create(Paths.get("./noEvent.jfc"));
        try (RecordingStream stream = new RecordingStream(config)) {
            CountDownLatch latch = new CountDownLatch(3);
            stream.enable("quarkus.InfinispanMultiEntry");
            stream.enable("quarkus.InfinispanMultiEntryStart");
            stream.enable("quarkus.InfinispanMultiEntryEnd");
            stream.onEvent(e -> {
                latch.countDown();
                Assertions.assertNotNull(e.getString("traceId"));
                Assertions.assertEquals(DEFAULT_CACHE_NAME, e.getString("cacheName"));
                Assertions.assertEquals(DEFAULT_CLUSTER, e.getString("clusterName"));
                Assertions.assertEquals("getAllAsync", e.getString("method"));
                Assertions.assertEquals(2, e.getInt("elementCount"));
            });
            stream.startAsync();

            remoteCache.getAllAsync(Set.of("key1", "key2"));

            Assertions.assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    void testCacheWide() throws IOException, ParseException, InterruptedException {
        Configuration config = Configuration.create(Paths.get("./noEvent.jfc"));
        try (RecordingStream stream = new RecordingStream(config)) {
            CountDownLatch latch = new CountDownLatch(3);
            stream.enable("quarkus.InfinispanCacheWide");
            stream.enable("quarkus.InfinispanCacheWideStart");
            stream.enable("quarkus.InfinispanCacheWideEnd");
            stream.onEvent(e -> {
                latch.countDown();
                Assertions.assertNotNull(e.getString("traceId"));
                Assertions.assertEquals(DEFAULT_CACHE_NAME, e.getString("cacheName"));
                Assertions.assertEquals(DEFAULT_CLUSTER, e.getString("clusterName"));
                Assertions.assertEquals("clearAsync", e.getString("method"));
            });
            stream.startAsync();

            remoteCache.clearAsync();

            Assertions.assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    void testAsyncException() throws IOException, ParseException, InterruptedException {
        Configuration config = Configuration.create(Paths.get("./noEvent.jfc"));
        try (RecordingStream stream = new RecordingStream(config)) {
            stream.enable("quarkus.InfinispanSingleEntry");
            stream.enable("quarkus.InfinispanSingleEntryStart");
            stream.enable("quarkus.InfinispanSingleEntryEnd");
            CountDownLatch latch = new CountDownLatch(3);
            stream.onEvent(e -> {
                latch.countDown();
                Assertions.assertNotNull(e.getString("traceId"));
                Assertions.assertEquals(DEFAULT_CACHE_NAME, e.getString("cacheName"));
                Assertions.assertEquals(DEFAULT_CLUSTER, e.getString("clusterName"));
                Assertions.assertEquals("computeAsync", e.getString("method"));
            });
            stream.startAsync();

            try {
                remoteCache.computeAsync("key", (k, v) -> {
                    throw new RuntimeException();
                }).join();
                Assertions.fail("Expected exception");
            } catch (RuntimeException e) {
                // expected
            }

            Assertions.assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }
}
