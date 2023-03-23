package io.quarkus.reactive.mssql.client;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class MSSQLPoolLoadBalanceTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DevModeResource.class)
                    .add(new StringAsset("quarkus.datasource.db-kind=mssql\n" +
                            "quarkus.datasource.reactive.url=vertx-reactive:sqlserver://localhost:9434/load_balance_test," +
                            "vertx-reactive:sqlserver://localhost:9435/load_balance_test," +
                            "vertx-reactive:sqlserver://localhost:9436/load_balance_test"),
                            "application.properties"));

    @Test
    public void testLoadBalance() {
        RestAssured
                .get("/dev/error")
                .then()
                .statusCode(200)
                .body(Matchers.anyOf(Matchers.endsWith(":9434"), Matchers.endsWith(":9435"), Matchers.endsWith(":9436")));
    }
}
