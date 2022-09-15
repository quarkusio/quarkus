package io.quarkus.flyway.test;

import static org.hamcrest.Matchers.is;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class FlywayDevModeModifyMigrationTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RowCountEndpoint.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("clean-and-migrate-at-start-config.properties", "application.properties"));

    @Test
    public void testModifyingExistingMigrationScriptCausesRestart() {
        RestAssured.get("/row-count").then().statusCode(200).body(is("0"));
        config.modifyResourceFile("db/migration/V1.0.0__Quarkus.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s + '\n' + "INSERT INTO quarked_flyway VALUES (1001, 'test')";
            }
        });
        RestAssured.get("/row-count").then().statusCode(200).body(is("1"));
    }

    @Path("/row-count")
    public static class RowCountEndpoint {

        @Inject
        AgroalDataSource dataSource;

        @GET
        public int rowCount() throws SQLException {
            try (Connection connection = dataSource.getConnection(); Statement stat = connection.createStatement()) {
                try (ResultSet countQuery = stat.executeQuery("select count(1) from quarked_flyway")) {
                    return countQuery.first() ? countQuery.getInt(1) : 0;
                }
            }
        }
    }
}
