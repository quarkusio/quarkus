package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FullGitInfoTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withEmptyApplication()
            .overrideConfigKey("quarkus.info.git.mode", "full");

    @Test
    public void test() {
        when().get("/q/info").then().statusCode(200).body("os", is(notNullValue())).body("java", is(notNullValue()))
                .body("build", is(notNullValue())).body("git", is(notNullValue()))
                .body("git.branch", is(notNullValue())).body("git.build", is(notNullValue()))
                .body("git.commit", is(notNullValue())).body("git.commit.author", is(notNullValue()))
                .body("git.commit.committer", is(notNullValue())).body("git.tags", is(notNullValue()));

    }
}
