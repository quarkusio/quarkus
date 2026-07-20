package io.quarkus.vertx.http.tls;

import static org.assertj.core.api.Fail.fail;

import java.io.File;
import java.net.URL;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-jdkssl-strict-invalid-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@DisabledIf("isJdk27OrLater")
public class HybridKeyExchangeJdkSslStrictInvalidTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/jdkssl-strict-invalid", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-jdkssl-strict-invalid-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-jdkssl-strict-invalid-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "strict")
            .overrideConfigKey("quarkus.tls.ssl-engine", "jdkssl")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled")
            .assertException(t -> {
            });

    /*
     * On jdk < 27, no pq-compliant groups are available. As such, vertx doesn't accept to start a server with a strict
     * policy when jdkssl is forced on jdk < 27.
     * TODO when pqc is backported to java 25, update condition
     */
    @Test
    void test() {
        fail("Should not be called, server startup should have failed.");
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/jdkssl-strict-invalid").handler(rc -> rc.response().end("jdkssl-strict-invalid-ok"));
        }

    }
}
