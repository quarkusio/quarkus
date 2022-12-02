package io.quarkus.jdbc.postgresql.deployment;

import java.util.logging.Level;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevServicesPostgresqlDatasourceDevModeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(PgResource.class)
                    .addAsResource(new StringAsset(""), "application.properties"))
            // Expect no warnings (in particular from Agroal)
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    // There are other warnings: JDK8, TestContainers, drivers, ...
                    // Ignore them: we're only interested in Agroal here.
                    && record.getMessage().contains("Agroal"));

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testDatasource() throws Exception {
        RestAssured.get("/pg/save?name=foo&value=bar")
                .then().statusCode(204);

        RestAssured.get("/pg/get?name=foo")
                .then().statusCode(200)
                .body(Matchers.equalTo("bar"));

        test.modifyResourceFile("application.properties", s -> "quarkus.datasource.devservices.properties.log=TRACE");

        RestAssured.get("/pg/get?name=foo")
                .then().statusCode(404);
        RestAssured.get("/pg/save?name=foo&value=bar")
                .then().statusCode(204);
        RestAssured.get("/pg/get?name=foo")
                .then().statusCode(200)
                .body(Matchers.equalTo("bar"));
    }
}
