package io.quarkus.vertx.http.security;

import java.io.File;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.vertx.http.runtime.security.MtlsAuthenticationMechanism;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = {
        Format.JKS }, client = true))
public class InclusiveAuthValidationTest {

    private static final String configuration = """
            quarkus.http.auth.inclusive=true
            quarkus.http.ssl.certificate.key-store-file=server-keystore.jks
            quarkus.http.ssl.certificate.key-store-password=secret
            quarkus.http.ssl.certificate.trust-store-file=server-truststore.jks
            quarkus.http.ssl.certificate.trust-store-password=secret
            quarkus.http.ssl.client-auth=REQUEST
            quarkus.http.auth.basic=true
            quarkus.http.auth.proactive=true
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TopPriorityAuthMechanism.class, StartupObserver.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File("target/certs/mtls-test-keystore.jks"), "server-keystore.jks")
                    .addAsResource(new File("target/certs/mtls-test-server-truststore.jks"), "server-truststore.jks"))
            .assertException(throwable -> {
                var errMsg = throwable.getMessage();
                Assertions.assertTrue(errMsg.contains("Inclusive authentication is enabled"));
                Assertions.assertTrue(errMsg.contains("TopPriorityAuthMechanism"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    public static class StartupObserver {
        void observe(@Observes StartupEvent startupEvent, HttpAuthenticator authenticator) {
            // authenticator is only initialized when required
        }
    }

    @ApplicationScoped
    public static class TopPriorityAuthMechanism implements HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return Uni.createFrom().nullItem();
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return Set.of(UsernamePasswordAuthenticationRequest.class);
        }

        @Override
        public int getPriority() {
            return MtlsAuthenticationMechanism.INCLUSIVE_AUTHENTICATION_PRIORITY + 1;
        }
    }
}
