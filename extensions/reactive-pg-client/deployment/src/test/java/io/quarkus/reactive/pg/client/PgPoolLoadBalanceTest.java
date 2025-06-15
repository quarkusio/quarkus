package io.quarkus.reactive.pg.client;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class PgPoolLoadBalanceTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest().withApplicationRoot((jar) -> jar
            .addClass(DevModeResource.class)
            .add(new StringAsset("quarkus.datasource.db-kind=postgresql\n"
                    + "quarkus.datasource.reactive.url=vertx-reactive:postgresql://localhost:5342/load_balance_test,"
                    + "vertx-reactive:postgresql://localhost:5343/load_balance_test,"
                    + "vertx-reactive:postgresql://localhost:5344/load_balance_test"), "application.properties"));

    @Test
    public void testLoadBalance() {
        RestAssured.get("/dev/error").then().statusCode(200).body(
                Matchers.anyOf(Matchers.endsWith(":5342"), Matchers.endsWith(":5343"), Matchers.endsWith(":5344")));
    }
}
