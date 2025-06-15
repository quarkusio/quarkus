package io.quarkus.reactive.oracle.client;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Disabled("Failing on CI but working locally - must be investigated")
public class ReactiveOracleReloadTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(DevModeResource.class)
                    .add(new StringAsset("quarkus.datasource.db-kind=oracle\n" + "quarkus.datasource.username=SYSTEM\n"
                            + "quarkus.datasource.password=hibernate_orm_test\n"
                            + "quarkus.datasource.reactive.url=vertx-reactive:oracle:thin:@localhost:2115/FREEPDB1"),
                            "application.properties"));

    @Test
    public void testHotReplacement() {
        RestAssured.get("/dev/error").then().statusCode(200);

        test.modifyResourceFile("application.properties", s -> s.replace(":2115", ":1521"));

        RestAssured.get("/dev/connected").then().statusCode(200);
    }
}
