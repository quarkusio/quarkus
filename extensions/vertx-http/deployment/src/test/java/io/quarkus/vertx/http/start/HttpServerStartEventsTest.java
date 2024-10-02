package io.quarkus.vertx.http.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.ObservesAsync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.HttpServerStart;
import io.quarkus.vertx.http.HttpsServerStart;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class HttpServerStartEventsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(MyListener.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-file", "server-keystore.jks")
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-password", "secret");

    @Test
    public void test() throws InterruptedException {
        assertTrue(MyListener.HTTP.await(5, TimeUnit.SECONDS));
        assertTrue(MyListener.HTTPS.await(5, TimeUnit.SECONDS));
        // httpsStarted() is static
        assertEquals(1, MyListener.COUNTER.get());
    }

    @Dependent
    public static class MyListener {

        static final AtomicInteger COUNTER = new AtomicInteger();
        static final CountDownLatch HTTP = new CountDownLatch(1);
        static final CountDownLatch HTTPS = new CountDownLatch(1);

        void httpStarted(@ObservesAsync HttpServerStart start) {
            assertNotNull(start.options());
            HTTP.countDown();
        }

        static void httpsStarted(@ObservesAsync HttpsServerStart start) {
            assertNotNull(start.options());
            HTTPS.countDown();
        }

        @PreDestroy
        void destroy() {
            COUNTER.incrementAndGet();
        }

    }

}
