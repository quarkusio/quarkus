package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * The post-authentication hook marks its own rejections like the pre-authentication one, and, under lazy
 * authentication, also the 401 Quarkus answers when the identity it forces fails to resolve; a served request stays
 * sampled.
 */
public class LoadSheddingInteractionPostAuthenticationTest {

    private static final String PREFIX = "quarkus.http.rate-limit.auth.";

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.withUsers(
            LoadSheddingInteractionApp.adaptive(LoadSheddingInteractionApp.create()))
            .overrideConfigKey("quarkus.http.auth.proactive", "false")
            .overrideConfigKey(PREFIX + "paths", "/auth/*")
            .overrideConfigKey(PREFIX + "phase", "post-authentication")
            .overrideConfigKey(PREFIX + "request-key", "principal")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .setForcedDependencies(Stream.concat(LoadSheddingInteractionApp.DEPENDENCIES.stream(),
                    Stream.of(Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment",
                            Version.getVersion())))
                    .toList());

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    /**
     * Fails if the 429s were sampled, for the reason given in {@link LoadSheddingInteractionFloodTest}.
     */
    @Test
    void postAuthenticationRejectionSkipsTheSampleToo() throws Exception {
        WebClient client = LoadSheddingInteractionApp.client(vertx);
        try {
            Map<String, String> alice = TestHttp.basic("alice", "alice");
            assertThat(LoadSheddingInteractionApp.get(vertx, client, url, "/auth/x", alice).statusCode())
                    .isEqualTo(200);
            for (int i = 0; i < 50; i++) {
                assertThat(LoadSheddingInteractionApp.get(vertx, client, url, "/auth/x", alice).statusCode())
                        .isEqualTo(429);
            }

            LoadSheddingInteractionApp.normalTraffic(vertx, client, url);
            LoadSheddingInteractionApp.assertDetectorUndisturbed(vertx, url);
        } finally {
            client.close();
        }
    }

    @Test
    void lazyAuthenticationFailureIsMarkedAndAServedRequestIsNot() throws Exception {
        WebClient client = LoadSheddingInteractionApp.client(vertx);
        try {
            assertThat(LoadSheddingInteractionApp.get(vertx, client, url, "/auth/x",
                    LoadSheddingInteractionApp.probe("401", TestHttp.basic("bob", "wrong"))).statusCode()).isEqualTo(401);
            assertThat(LoadSheddingInteractionApp.get(vertx, client, url, "/auth/x",
                    LoadSheddingInteractionApp.probe("200", TestHttp.basic("bob", "bob"))).statusCode()).isEqualTo(200);
            assertThat(LoadSheddingInteractionApp.get(vertx, client, url, "/auth/x",
                    LoadSheddingInteractionApp.probe("429", TestHttp.basic("bob", "bob"))).statusCode()).isEqualTo(429);

            assertThat(LoadSheddingInteractionApp.recorded("401")).contains(true);
            // the hook removes its marking once the identity resolves
            assertThat(LoadSheddingInteractionApp.recorded("200")).isEmpty();
            assertThat(LoadSheddingInteractionApp.recorded("429")).contains(true);
        } finally {
            client.close();
        }
    }
}
