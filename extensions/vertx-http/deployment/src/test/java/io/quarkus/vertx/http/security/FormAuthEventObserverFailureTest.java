package io.quarkus.vertx.http.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent;
import io.restassured.RestAssured;

public class FormAuthEventObserverFailureTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.form.enabled=true\n" +
            "quarkus.http.auth.form.login-page=login\n" +
            "quarkus.http.auth.form.error-page=error\n" +
            "quarkus.http.auth.form.landing-page=landing\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=admin\n" +
            "quarkus.http.auth.permission.roles1.paths=/admin\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                            PathHandler.class, ThrowingFormAuthEventObserver.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testThrowingObserverDoesNotHangLogin() {
        CompletableFuture<Integer> status = CompletableFuture.supplyAsync(() -> RestAssured
                .given()
                .redirects().follow(false)
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .statusCode());
        // The login fails like any other post-authentication failure instead of hanging
        assertThat(status)
                .succeedsWithin(Duration.ofSeconds(10))
                .isEqualTo(500);
    }

    public static class ThrowingFormAuthEventObserver {

        void observe(@Observes FormAuthenticationEvent event) {
            throw new IllegalStateException("boom");
        }
    }
}
