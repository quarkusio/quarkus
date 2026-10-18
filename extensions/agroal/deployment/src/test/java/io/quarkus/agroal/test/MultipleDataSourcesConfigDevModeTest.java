package io.quarkus.agroal.test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class MultipleDataSourcesConfigDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MultipleDataSourcesTestUtil.class, DevModeTestEndpoint.class)
                    .addAsResource("application-multiple-datasources.properties", "application.properties"));

    @Test
    public void testDataSourceInjection() throws Exception {
        runTest("default", "jdbc:h2:tcp://localhost/mem:default", "username-default", 3, 13);
        runTest("users", "jdbc:h2:tcp://localhost/mem:users", "username1", 1, 11);
        runTest("inventory", "jdbc:h2:tcp://localhost/mem:inventory", "username2", 2, 12);
        config.modifyResourceFile("application.properties", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("quarkus.datasource.inventory.jdbc.max-size=12",
                        "quarkus.datasource.inventory.jdbc.max-size=19");
            }
        });
        runTest("inventory", "jdbc:h2:tcp://localhost/mem:inventory", "username2", 2, 19);
    }

    static void runTest(String dataSourceName, String jdbcUrl, String username,
            int minSize, int maxSize) throws UnsupportedEncodingException {
        RestAssured
                .get("/test/" + dataSourceName + "/" + URLEncoder.encode(jdbcUrl, StandardCharsets.UTF_8.name()) + "/"
                        + username + "/" + maxSize)
                .then()
                .statusCode(200).body(Matchers.equalTo("ok"));
    }

}
