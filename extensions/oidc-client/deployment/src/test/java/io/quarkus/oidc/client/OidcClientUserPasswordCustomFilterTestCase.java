package io.quarkus.oidc.client;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmUserPasswordCustomFilterManager.class)
public class OidcClientUserPasswordCustomFilterTestCase {

    private static Class<?>[] testClasses = {
            FrontendResource.class,
            ProtectedResource.class,
            ProtectedResourceService.class,
            OidcClientRequestCustomFilter.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-custom-filter.properties", "application.properties"));

    @Test
    public void testGetUserName() {
        RestAssured.when().get("/frontend/user")
                .then()
                .statusCode(200)
                .body(equalTo("bob"));

    }

    @Test
    public void testGetUserOidcClientNameAndRefreshTokens() {
        RestAssured.when().get("/frontend/user")
                .then()
                .statusCode(200)
                .body(equalTo("bob"));

        // Wait until the access token has expired
        long expiredTokenTime = System.currentTimeMillis() + 5000;
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(3))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return System.currentTimeMillis() > expiredTokenTime;
                    }
                });

        RestAssured.when().get("/frontend/user")
                .then()
                .statusCode(200)
                .body(equalTo("bob"));
    }
}
