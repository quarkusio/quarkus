package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.info.BuildInfo;
import io.quarkus.info.GitInfo;
import io.quarkus.info.JavaInfo;
import io.quarkus.info.OsInfo;
import io.quarkus.test.QuarkusUnitTest;

public class EnabledInfoTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    GitInfo gitInfo;

    @Inject
    BuildInfo buildInfo;

    @Inject
    OsInfo osInfo;

    @Inject
    JavaInfo javaInfo;

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
                .body("build.quarkusVersion", is(notNullValue()))
                .body("git", is(notNullValue()))
                .body("git.branch", is(notNullValue()))
                .body("git.build", is(nullValue()));

        assertNotNull(buildInfo);
        assertNotNull(buildInfo.group());
        assertNotNull(buildInfo.artifact());
        assertNotNull(buildInfo.version());
        assertNotNull(buildInfo.time());
        assertNotNull(buildInfo.quarkusVersion());

        assertNotNull(gitInfo);
        assertNotNull(gitInfo.branch());
        assertNotNull(gitInfo.latestCommitId());
        assertNotNull(gitInfo.commitTime());

        assertNotNull(osInfo);
        assertNotNull(osInfo.name());
        assertNotNull(osInfo.version());
        assertNotNull(osInfo.architecture());

        assertNotNull(javaInfo);
        assertNotNull(javaInfo.version());
        assertNotNull(javaInfo.vendor());
        assertNotNull(javaInfo.vendorVersion());
    }
}
