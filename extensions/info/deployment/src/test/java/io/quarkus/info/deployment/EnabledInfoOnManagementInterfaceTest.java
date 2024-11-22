package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EnabledInfoOnManagementInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.management.enabled", "true");

    @Test
    public void test() {
        when().get("/q/info")
                .then()
                .statusCode(404);

        when().get("http://localhost:9001/q/info")
                .then()
                .statusCode(200)
                .body("os", is(notNullValue()))
                .body("os.name", is(notNullValue()))
                .body("java", is(notNullValue()))
                .body("java.version", is(notNullValue()))
                .body("build", is(notNullValue()))
                .body("build.time", is(notNullValue()))
                .body("git", is(notNullValue()))
                .body("git.branch", is(notNullValue()));

    }
}
