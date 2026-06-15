package io.quarkus.resteasy.reactive.server.test.stress;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.PoolOptions;

public class DrainTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEndpoint.class, BytesData.class, Data.class, DataBodyWriter.class)
                    .addAsResource("server-keystore.jks"))
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-file", "server-keystore.jks")
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-password", "secret");

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testAsyncHttp1() throws Exception {
        runTest("/test/bytesAsync", createHttp1Options());
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testSyncHttp1() throws Exception {
        runTest("/test/bytesSync", createHttp1Options());
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testAsyncHttp2() throws Exception {
        runTest("/test/bytesAsync", createHttp2Options());
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testSyncHttp2() throws Exception {
        runTest("/test/bytesSync", createHttp2Options());
    }

    private void runTest(String path, HttpClientOptions options) throws Exception {
        int num = 10_000;
        Vertx vertx = Vertx.vertx();
        try {
            HttpClient client = vertx.createHttpClient(options, new PoolOptions().setHttp1MaxSize(64));
            CountDownLatch latch = new CountDownLatch(num);
            AtomicLong sum = new AtomicLong();
            AtomicReference<Throwable> failure = new AtomicReference<>();

            for (int i = 0; i < num; i++) {
                client.request(HttpMethod.GET, path)
                        .compose(HttpClientRequest::send)
                        .compose(HttpClientResponse::body)
                        .onSuccess(body -> {
                            sum.addAndGet(body.length());
                            latch.countDown();
                        })
                        .onFailure(err -> {
                            failure.compareAndSet(null, err);
                            latch.countDown();
                        });
            }

            Assertions.assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
            if (failure.get() != null) {
                Assertions.fail("Request failed", failure.get());
            }
            Assertions.assertThat(sum.get()).isEqualTo(1_000_000_000L);
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get();
        }
    }

    private static HttpClientOptions createHttp1Options() {
        return new HttpClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(8444)
                .setSsl(true)
                .setTrustAll(true);
    }

    private static HttpClientOptions createHttp2Options() {
        return new HttpClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(8444)
                .setSsl(true)
                .setTrustAll(true)
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2);
    }

    public interface Data {

        byte[] bytes();

    }

    public record BytesData(byte[] bytes) implements Data {

    }

    private static final Data DATA = new BytesData(new byte[100_000]);

    @Path("/test")
    public static class MyEndpoint {
        @GET
        @Path("bytesAsync")
        @Produces({ "application/json", "application/cbor" }) // Not really what is produced...
        public Uni<Data> getBytesAsync() {
            return Uni.createFrom().item(DATA);
        }

        @GET
        @Path("bytesSync")
        @Produces({ "application/json", "application/cbor" }) // Not really what is produced...
        public Data getBytesSync() {
            return DATA;
        }
    }

    @Provider
    @ApplicationScoped
    @Produces({ "application/json", "application/cbor" })
    public static class DataBodyWriter implements MessageBodyWriter<Data> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Data.class.isAssignableFrom(type);
        }

        @Override
        public void writeTo(Data data, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            entityStream.write(data.bytes());
        }

    }

}
