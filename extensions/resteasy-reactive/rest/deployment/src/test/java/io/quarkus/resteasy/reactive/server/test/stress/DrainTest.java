package io.quarkus.resteasy.reactive.server.test.stress;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class DrainTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEndpoint.class, BytesData.class, Data.class, DataBodyWriter.class)
                    .addAsResource("server-keystore.jks"))
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-file", "server-keystore.jks")
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-password", "secret");

    HttpClient client;

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testAsyncHttp1() {
        client = createJavaHttpClient();
        long before = System.currentTimeMillis();
        var sum = IntStream.range(0, 10000)
                .parallel()
                .map(i -> get("https://localhost:8444/test/bytesAsync"))
                .sum();
        System.out.println("Request completed in " + (System.currentTimeMillis() - before) + " ms");
        Assertions.assertThat(sum).isEqualTo(1000000000);
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testSyncHttp1() {
        client = createJavaHttpClient();
        long before = System.currentTimeMillis();
        var sum = IntStream.range(0, 10000)
                .parallel()
                .map(i -> get("https://localhost:8444/test/bytesSync"))
                .sum();
        System.out.println("Request completed in " + (System.currentTimeMillis() - before) + " ms");
        Assertions.assertThat(sum).isEqualTo(1000000000);
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testAsyncHttp2() {
        client = createJavaHttp2Client();
        long before = System.currentTimeMillis();
        var sum = IntStream.range(0, 10000)
                .parallel()
                .map(i -> get("https://localhost:8444/test/bytesAsync"))
                .peek(i -> System.out.println(Instant.now() + " Got response: " + i))
                .sum();
        System.out.println("Request completed in " + (System.currentTimeMillis() - before) + " ms");
        Assertions.assertThat(sum).isEqualTo(1000000000);
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void testSyncHttp2() {
        client = createJavaHttp2Client();
        long before = System.currentTimeMillis();
        var sum = IntStream.range(0, 10000)
                .parallel()
                .map(i -> get("https://localhost:8444/test/bytesSync"))
                .sum();
        System.out.println("Request completed in " + (System.currentTimeMillis() - before) + " ms");
        Assertions.assertThat(sum).isEqualTo(1000000000);
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

    int get(String uri) {
        var request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        var response = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).join();
        return response.body().length;
    }

    private static HttpClient createJavaHttpClient() {
        try {
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { AllowAllTrustManager.INSTANCE }, SecureRandom.getInstanceStrong());
            return HttpClient.newBuilder().sslContext(sslContext).build();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to create HTTP client");
        }
    }

    private static HttpClient createJavaHttp2Client() {
        try {
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { AllowAllTrustManager.INSTANCE }, SecureRandom.getInstanceStrong());
            return HttpClient.newBuilder().sslContext(sslContext).version(HttpClient.Version.HTTP_2).build();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to create HTTP client");
        }
    }

    private enum AllowAllTrustManager implements X509TrustManager {
        INSTANCE;

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // do nothing
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // do nothing
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
