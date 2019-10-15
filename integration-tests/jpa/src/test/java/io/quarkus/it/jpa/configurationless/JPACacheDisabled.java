package io.quarkus.it.jpa.configurationless;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Similar to JPAConfigurationlessTest, yet explicitly disabling 2nd level caching,
 * then asserting the TimestampsCacheDisabledImpl have been disabled as a consequence.
 */
public class JPACacheDisabled {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "false");

    @Test
    public void testInjection() {
        RestAssured.when().get("/jpa-test").then()
                .body(containsString("jpa=OK"));

        RestAssured.when().get("/jpa-test/user-tx").then()
                .body(containsString("jpa=OK"));

        RestAssured.when().get("/jpa-test/timestamps").then()
                .body(containsString("org.hibernate.cache.internal.TimestampsCacheDisabledImpl"));
    }

}
