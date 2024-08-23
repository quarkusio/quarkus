package io.quarkus.websockets.next.test.security;

import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;

public class HttpUpgradeAnnotationTransformerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, WSClient.class, SecurityTestUtils.class, IdentityMock.class,
                            CdiBeanSecurity.class, AdminEndpoint.class));

    @Inject
    CdiBeanSecurity cdiBeanSecurity;

    @Test
    public void testSecurityChecksNotRepeated() {
        // fact that HTTP Upgrade is secured is tested in HttpUpgradeRolesAllowedAnnotationTest
        // this test class complements these tests but must stand separately as it relies on different auth

        // when HTTP Upgrade is secured, we should not perform over and over again
        // same check @OnTextMessage
        var admin = new AuthData(Set.of("admin"), false, "admin");
        var user = new AuthData(Set.of("user"), false, "user");
        var anonymous = new AuthData(Set.of(), true, "anonymous");

        // both HTTP Upgrade and @OnTextMessage are secured
        assertSuccess(cdiBeanSecurity::httpUpgradeAndCdiBeanSecurity, "hey", admin);
        assertFailureFor(cdiBeanSecurity::httpUpgradeAndCdiBeanSecurity, ForbiddenException.class, user);
        assertFailureFor(cdiBeanSecurity::httpUpgradeAndCdiBeanSecurity, UnauthorizedException.class, anonymous);

        // only HTTP Upgrade is secured -> no CDI bean security
        assertSuccess(cdiBeanSecurity::httpUpgradeSecurity, "hey", admin);
        assertSuccess(cdiBeanSecurity::httpUpgradeSecurity, "hey", user);
        assertSuccess(cdiBeanSecurity::httpUpgradeSecurity, "hey", anonymous);
    }

    @ApplicationScoped
    public static class CdiBeanSecurity {

        @Inject
        AdminEndpoint adminEndpoint;

        @Inject
        Endpoint endpoint;

        public String httpUpgradeSecurity() {
            return adminEndpoint.echo("hey");
        }

        public String httpUpgradeAndCdiBeanSecurity() {
            return endpoint.echo("hey");
        }

    }

    @RolesAllowed("admin")
    @WebSocket(path = "/admin-end")
    public static class AdminEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

    }

    @RolesAllowed({ "admin", "user" })
    @WebSocket(path = "/end")
    public static class Endpoint {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @RolesAllowed("admin")
        @OnTextMessage
        String echo(String message) {
            if (!currentIdentity.getIdentity().hasRole("admin")) {
                throw new IllegalStateException();
            }
            return message;
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
        }

    }
}
