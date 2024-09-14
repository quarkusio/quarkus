package io.quarkus.vertx.http.security;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.Log;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;

/**
 * Inspired by https://github.com/quarkusio/quarkus/issues/43217.
 * Tests that number of concurrent blocking requests processed
 * is not limited by a number of the IO threads.
 */
public class ConcurrentBlockingRequestTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String APP_PROPS = """
            quarkus.http.auth.permission.auth.paths=/blocker
            quarkus.http.auth.permission.auth.policy=root-policy
            quarkus.http.auth.policy.root-policy.roles-allowed=root
            quarkus.http.auth.permission.auth.paths=/viewer
            quarkus.http.auth.permission.auth.policy=viewer-policy
            quarkus.http.auth.policy.viewer-policy.roles-allowed=viewer
            quarkus.http.io-threads=1
            """;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class,
                            BlockingAugmentor.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Inject
    Vertx vertx;

    @TestHTTPResource("/blocker")
    URL blockerUri;

    @TestHTTPResource("/viewer")
    URL viewerUri;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("blocker", "blocker")
                .add("test", "test");
    }

    @Test
    public void testConcurrentBlockingExecutionAllowed() {
        var httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setDefaultHost(blockerUri.getHost())
                .setDefaultPort(blockerUri.getPort()));
        try {
            // first perform blocking request
            AtomicBoolean blockerSucceeded = new AtomicBoolean(false);
            AtomicBoolean blockerFailed = new AtomicBoolean(false);
            httpClient
                    .request(HttpMethod.GET, blockerUri.getPath())
                    .map(withBasic("blocker:blocker"))
                    .flatMap(HttpClientRequest::send)
                    .subscribe()
                    .with(resp -> {
                        if (resp.statusCode() == 200) {
                            resp.body().map(Buffer::toString).subscribe().with(body -> {
                                if (body.equals("blocker:/blocker")) {
                                    blockerSucceeded.set(true);
                                } else {
                                    Log.error(("Request to path '/blocker' failed, expected response body 'blocker:/blocker',"
                                            + " got: %s").formatted(body));
                                    blockerFailed.set(true);
                                }
                            });
                        } else {
                            Log.error("Request to path '/blocker' failed, expected response status 200, got: "
                                    + resp.statusCode());
                            blockerFailed.set(true);
                        }
                    }, err -> {
                        Log.error("Request to path '/blocker' failed", err);
                        blockerFailed.set(true);
                    });

            assertFalse(blockerSucceeded::get);
            assertFalse(blockerFailed::get);

            // anonymous request is denied while blocking is still in progress
            int statusCode = requestToViewerPathAndGetStatusCode(httpClient, null);
            assertEquals(401, statusCode);
            assertFalse(blockerSucceeded::get);
            assertFalse(blockerFailed::get);

            int concurrentAuthReq = BlockingAugmentor.EXPECTED_AUTHENTICATED_REQUESTS;
            do {
                assertFalse(blockerSucceeded::get);
                assertFalse(blockerFailed::get);
                statusCode = requestToViewerPathAndGetStatusCode(httpClient, "test:test");
                assertEquals(200, statusCode);
            } while (--concurrentAuthReq > 0);

            Awaitility.await().untilAsserted(() -> {
                assertTrue(blockerSucceeded::get);
                assertFalse(blockerFailed::get);
            });
        } finally {
            httpClient.closeAndAwait();
        }
    }

    private int requestToViewerPathAndGetStatusCode(HttpClient httpClient, String credentials) {
        var response = httpClient
                .request(HttpMethod.GET, viewerUri.getPath())
                .map(withBasic(credentials))
                .flatMap(HttpClientRequest::send)
                .await().atMost(REQUEST_TIMEOUT);
        return response.statusCode();
    }

    private static Function<HttpClientRequest, HttpClientRequest> withBasic(String basic) {
        if (basic != null) {
            return req -> req.putHeader("Authorization",
                    "Basic " + encodeBase64URLSafeString(basic.getBytes(StandardCharsets.UTF_8)));
        }
        return Function.identity();
    }

    @ApplicationScoped
    public static class BlockingAugmentor implements SecurityIdentityAugmentor {

        static final int EXPECTED_AUTHENTICATED_REQUESTS = 3;
        private final CountDownLatch blockingLatch = new CountDownLatch(EXPECTED_AUTHENTICATED_REQUESTS);

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity, AuthenticationRequestContext ctx) {
            if (!securityIdentity.isAnonymous()) {
                if ("blocker".equals(securityIdentity.getPrincipal().getName())) {
                    return ctx.runBlocking(() -> {
                        try {
                            Log.info("Waiting for next 3 authenticated requests before continuing");
                            boolean concurrentRequestsDone = blockingLatch.await(15, TimeUnit.SECONDS);
                            if (concurrentRequestsDone) {
                                Log.info("Waiting ended, adding role 'blocker' to SecurityIdentity");
                                return withRole(securityIdentity, "blocker");
                            } else {
                                Log.error("Waiting ended, concurrent authenticated requests were not detected");
                                return securityIdentity;
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    Log.info("Detected authenticated identity, adding role 'viewer'");
                    blockingLatch.countDown();
                    return ctx.runBlocking(() -> withRole(securityIdentity, "viewer"));
                }
            }
            Log.info("Detected anonymous identity - no augmentation.");
            return Uni.createFrom().item(securityIdentity);
        }

        private static SecurityIdentity withRole(SecurityIdentity securityIdentity, String role) {
            return QuarkusSecurityIdentity.builder(securityIdentity).addRole(role).build();
        }
    }

}
