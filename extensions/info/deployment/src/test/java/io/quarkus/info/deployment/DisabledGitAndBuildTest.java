package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DisabledGitAndBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.info.git.enabled", "false")
            .overrideConfigKey("quarkus.info.build.enabled", "false");

    @Test
    public void test() {
        when().get("/q/info")
                .then()
                .statusCode(200)
                .body("os", is(notNullValue()))
                .body("java", is(notNullValue()))
                .body("build", is(nullValue()))
                .body("git", is(nullValue()));

    }
}
