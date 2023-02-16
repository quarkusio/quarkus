package io.quarkus.resteasy.reactive.server.test.security;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class Http2FormAuthRedirectTestCase {

    @TestHTTPResource(value = "/j_security_check", ssl = true)
    URL sslUrl;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.form.enabled=true\n" +
                            "quarkus.http.insecure-requests=disabled\n" +
                            "quarkus.http.ssl.certificate.key-store-file=server-keystore.jks\n" +
                            "quarkus.http.ssl.certificate.key-store-password=secret"), "application.properties")
                    .addAsResource("server-keystore.jks");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("a d m i n", "a d m i n", "a d m i n");
    }

    @Test
    public void testFormAuthFailure() {
        Vertx vertx = Vertx.vertx();
        try {
            WebClientOptions options = new WebClientOptions()
                    .setSsl(true)
                    .setVerifyHost(false)
                    .setTrustAll(true)
                    .setProtocolVersion(HttpVersion.HTTP_2)
                    .setFollowRedirects(false)
                    .setUseAlpn(true);
            CompletableFuture<Integer> result = new CompletableFuture<>();
            MultiMap formParams = new QuarkusHttpHeaders()
                    .add("j_username", "a d m i n")
                    .add("j_password", "wrongpassword");
            WebClient.create(vertx, options)
                    .post(sslUrl.getPort(), sslUrl.getHost(), sslUrl.getPath())
                    .sendForm(formParams, ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<Buffer> response = ar.result();
                            result.complete(response.statusCode());
                        } else {
                            result.completeExceptionally(ar.cause());
                        }
                    });
            Assertions.assertEquals(302, result.get());
        } catch (ExecutionException | InterruptedException e) {
            Assertions.fail(e.getMessage());
        } finally {
            vertx.close();
        }
    }

}
