package io.quarkus.oidc.client;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.client.runtime.AbstractTokensProducer;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The periodic tokens refresh forces the acquisition of new tokens when the producer requires it, as the OIDC client
 * filters do once the access token has been rejected, so that the next request does not have to wait for them.
 */
@QuarkusTestResource(KeycloakRealmUserPasswordManager.class)
public class OidcClientRefreshIntervalForceNewTokensTestCase {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForceNewTokensProducer.class)
                    .addAsResource(new StringAsset(
                            "# Disable Dev Services, Keycloak is started by a Maven plugin\n"
                                    + "quarkus.keycloak.devservices.enabled=false\n"
                                    + "quarkus.oidc.tenant-enabled=false\n"
                                    + "quarkus.oidc-client.auth-server-url=${keycloak.url}/realms/quarkus/\n"
                                    + "quarkus.oidc-client.client-id=quarkus-app\n"
                                    + "quarkus.oidc-client.credentials.secret=secret\n"
                                    + "quarkus.oidc-client.grant.type=password\n"
                                    + "quarkus.oidc-client.grant-options.password.username=alice\n"
                                    + "quarkus.oidc-client.grant-options.password.password=alice\n"
                                    + "quarkus.oidc-client.refresh-interval=1s\n"),
                            "application.properties"));

    @Inject
    ForceNewTokensProducer producer;

    @Test
    public void testPeriodicRefreshForcesNewTokens() {
        Tokens initialTokens = producer.awaitTokens();

        producer.forceNewTokens.set(true);
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertFalse(producer.forceNewTokens.get(),
                        "the periodic tokens refresh should have forced the acquisition of new tokens"));

        assertNotEquals(initialTokens.getAccessToken(), producer.awaitTokens().getAccessToken());
    }

    @Singleton
    public static class ForceNewTokensProducer extends AbstractTokensProducer {

        final AtomicBoolean forceNewTokens = new AtomicBoolean();

        @Override
        protected boolean isForceNewTokens() {
            return forceNewTokens.getAndSet(false);
        }
    }
}
