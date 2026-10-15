package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.info.BuildInfo;
import io.quarkus.test.QuarkusExtensionTest;

public class DisabledQuarkusVersionInfoTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.info.build.quarkus-version", "false");

    @Inject
    BuildInfo buildInfo;

    @Test
    public void test() {
        when().get("/q/info")
                .then()
                .statusCode(200)
                .body("os", is(notNullValue()))
                .body("os.name", is(notNullValue()))
                .body("java", is(notNullValue()))
                .body("java.version", is(notNullValue()))
                .body("build", is(notNullValue()))
                .body("build.time", is(notNullValue()))
                .body("build.quarkusVersion", is(nullValue()))
                .body("git", is(notNullValue()))
                .body("git.branch", is(notNullValue()))
                .body("git.build", is(nullValue()));

        assertNotNull(buildInfo);
        assertNotNull(buildInfo.group());
        assertNotNull(buildInfo.artifact());
        assertNotNull(buildInfo.version());
        assertNotNull(buildInfo.time());
        assertNull(buildInfo.quarkusVersion());
    }
}
