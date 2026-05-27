package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.AbstractTrustedProxyDnTest.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.RoutingContext;

@Certificates(certificates = {
        @Certificate(name = TrustedProxyManagementInterfaceTest.CERT_NAME, aliases = {
                @Alias(name = "proxy-a", cn = "proxy-a", client = true, password = PASSWORD),
                @Alias(name = "proxy-b", cn = "proxy-b", client = true, password = PASSWORD)
        }, password = PASSWORD, formats = Format.PKCS12, subjectAlternativeNames = "DNS:localhost", client = true)
}, replaceIfExists = true, baseDir = "target/certs")
class TrustedProxyManagementInterfaceTest {

    static final String CERT_NAME = "proxy-mgmt-test";
    private static final String CERTS_DIR = "target/certs/";

    private static final String configuration = """
            quarkus.management.enabled=true
            quarkus.tls.key-store.p12.path=%1$s-keystore.p12
            quarkus.tls.key-store.p12.password=%2$s
            quarkus.tls.key-store.p12.alias=%3$s
            quarkus.tls.key-store.p12.alias-password=%2$s
            quarkus.tls.trust-store.p12.path=%1$s-server-truststore.p12
            quarkus.tls.trust-store.p12.password=%2$s
            quarkus.http.ssl.client-auth=required
            quarkus.http.proxy.proxy-address-forwarding=true
            quarkus.http.proxy.allow-forwarded=true
            quarkus.http.proxy.enable-trusted-proxy-header=true
            quarkus.http.proxy.trusted-proxy[0].subject-dn=CN=proxy-a
            quarkus.http.proxy.trusted-proxy[0].truststore-alias=proxy-a
            quarkus.management.ssl.client-auth=required
            quarkus.management.proxy.proxy-address-forwarding=true
            quarkus.management.proxy.allow-forwarded=true
            quarkus.management.proxy.enable-trusted-proxy-header=true
            quarkus.management.proxy.trusted-proxy[0].subject-dn=CN=proxy-b
            quarkus.management.proxy.trusted-proxy[0].truststore-alias=proxy-b
            """.formatted(CERTS_DIR + CERT_NAME, PASSWORD, CERT_NAME);

    @TestHTTPResource(value = "/trusted-proxy", tls = true)
    URL mainUrl;

    @TestHTTPResource(value = "/management-trusted-proxy", management = true, tls = true)
    URL managementUrl;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File(CERTS_DIR + CERT_NAME + "-keystore.p12"), "server-keystore.p12")
                    .addAsResource(new File(CERTS_DIR + CERT_NAME + "-server-truststore.p12"),
                            "server-truststore.p12"))
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    void proxyAAcceptedOnMainRouter() {
        String body = requestWithClientAlias("proxy-a", mainUrl);
        assertThat(body).isEqualTo("https|somehost|backend:4444|true");
    }

    @Test
    void proxyARejectedOnManagement() {
        String body = requestWithClientAlias("proxy-a", managementUrl);
        assertThat(body).startsWith("https|localhost").endsWith("|false");
    }

    @Test
    void proxyBRejectedOnMainRouter() {
        String body = requestWithClientAlias("proxy-b", mainUrl);
        assertThat(body).startsWith("https|localhost").endsWith("|false");
    }

    @Test
    void proxyBAcceptedOnManagement() {
        String body = requestWithClientAlias("proxy-b", managementUrl);
        assertThat(body).startsWith("https|somehost|").endsWith("|true");
    }

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                context.produce(buildItem.routeBuilder()
                        .management()
                        .route("management-trusted-proxy")
                        .handler(new ManagementTrustedProxyHandler())
                        .build());
            }
        }).produces(RouteBuildItem.class)
                .consumes(NonApplicationRootPathBuildItem.class)
                .build();
    }

    public static class ManagementTrustedProxyHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext rc) {
            rc.response().end(
                    rc.request().scheme() + "|" + rc.request().getHeader(HttpHeaders.HOST) + "|"
                            + rc.request().remoteAddress().toString()
                            + "|" + rc.request().getHeader("X-Forwarded-Trusted-Proxy"));
        }
    }

    private String requestWithClientAlias(String alias, URL targetUrl) {
        var options = new HttpClientOptions()
                .setSsl(true)
                .setDefaultPort(targetUrl.getPort())
                .setDefaultHost(targetUrl.getHost())
                .setKeyCertOptions(
                        new PfxOptions()
                                .setPath(CERTS_DIR + CERT_NAME + "-client-keystore.p12")
                                .setPassword(PASSWORD)
                                .setAlias(alias))
                .setTrustOptions(
                        new PfxOptions()
                                .setPath(CERTS_DIR + CERT_NAME + "-client-truststore.p12")
                                .setPassword(PASSWORD));

        var client = vertx.createHttpClient(options);
        try {
            return client
                    .request(HttpMethod.GET, targetUrl.getPath())
                    .map(req -> req.putHeader("Forwarded", "proto=https;for=backend:4444;host=somehost"))
                    .flatMap(HttpClientRequest::send)
                    .flatMap(HttpClientResponse::body)
                    .map(Buffer::toString)
                    .toCompletionStage().toCompletableFuture().join();
        } finally {
            client.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
