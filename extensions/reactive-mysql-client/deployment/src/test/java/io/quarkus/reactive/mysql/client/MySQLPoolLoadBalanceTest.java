package io.quarkus.reactive.mysql.client;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class MySQLPoolLoadBalanceTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(DevModeResource.class)
                    .add(new StringAsset("quarkus.datasource.db-kind=mysql\n"
                            + "quarkus.datasource.reactive.url=vertx-reactive:mysql://localhost:6033/load_balance_test,"
                            + "vertx-reactive:mysql://localhost:6034/load_balance_test,"
                            + "vertx-reactive:mysql://localhost:6035/load_balance_test"), "application.properties"));

    @Test
    public void testLoadBalance() {
        RestAssured.get("/dev/error").then().statusCode(200).body(
                Matchers.anyOf(Matchers.endsWith(":6033"), Matchers.endsWith(":6034"), Matchers.endsWith(":6035")));
    }
}
