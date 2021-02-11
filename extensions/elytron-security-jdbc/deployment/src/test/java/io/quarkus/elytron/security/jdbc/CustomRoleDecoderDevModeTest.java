package io.quarkus.elytron.security.jdbc;

import java.util.Arrays;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

//see https://github.com/quarkusio/quarkus/issues/9296
public class CustomRoleDecoderDevModeTest extends JdbcSecurityRealmTest {

    static Class[] testClassesWithCustomRoleDecoder = Stream.concat(
            Arrays.stream(testClasses),
            Arrays.stream(new Class[] { CustomRoleDecoder.class })).toArray(Class[]::new);

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClassesWithCustomRoleDecoder)
                    .addAsResource("custom-role-decoder/import.sql")
                    .addAsResource("custom-role-decoder/application.properties", "application.properties"));

    @Test
    public void testConfigChange() {
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/servlet-secured").then()
                .statusCode(200);
        //break the build time config
        config.modifyResourceFile("application.properties",
                s -> s.replace("quarkus.security.jdbc.principal-query.attribute-mappings.0.index=2",
                        "quarkus.security.jdbc.principal-query.attribute-mappings.0.index=3"));
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/servlet-secured").then()
                .statusCode(500);

        //now fix it again
        config.modifyResourceFile("application.properties",
                s -> s.replace("quarkus.security.jdbc.principal-query.attribute-mappings.0.index=3",
                        "quarkus.security.jdbc.principal-query.attribute-mappings.0.index=2"));
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }

}
