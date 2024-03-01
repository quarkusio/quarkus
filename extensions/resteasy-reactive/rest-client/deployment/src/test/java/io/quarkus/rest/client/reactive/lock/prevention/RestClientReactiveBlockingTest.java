package io.quarkus.rest.client.reactive.lock.prevention;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RestClientReactiveBlockingTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(TestClient.class.getPackage())
                    .addAsResource(new StringAsset(setUrlForClass(TestClient.class)),
                            "application.properties"));

    @Timeout(5) // it should end immediately, not after rest client timeout
    @Test
    void shouldFailToMakeABlockingCallFromNonBlockingContext() {
        // @formatter:off
        when()
                .get("/non-blocking/block")
        .then()
                .statusCode(500);
        // @formatter:on
    }

    @Test
    void shouldMakeNonBlockingCallFromNonBlockingContext() {
        // @formatter:off
        when()
                .get("/non-blocking/non-block")
        .then()
                .statusCode(200)
                .body(equalTo("something"));
        // @formatter:on
    }

    @Test
    void shouldMakeBlockingCallFromBlockingContext() {
        // @formatter:off
        when()
                .get("/non-blocking/block-properly")
        .then()
                .statusCode(200)
                .body(equalTo("something"));
        // @formatter:on
    }

}
