package io.quarkus.elytron.security.jdbc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.ContinuousTestingTestUtils.TestStatus;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

//see https://github.com/quarkusio/quarkus/issues/9296
public class CustomRoleDecoderDevModeTest extends JdbcSecurityRealmTest {

    static Class[] testClassesWithCustomRoleDecoder = Stream.concat(
            Arrays.stream(testClasses),
            Arrays.stream(new Class[] { CustomRoleDecoder.class })).toArray(Class[]::new);

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> {
                try (var in = CustomRoleDecoderDevModeTest.class.getClassLoader()
                        .getResourceAsStream("custom-role-decoder/application.properties")) {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(testClassesWithCustomRoleDecoder)
                            .addClasses(JdbcSecurityRealmTest.class)
                            .addAsResource("custom-role-decoder/import.sql")
                            .addAsResource(
                                    new StringAsset(ContinuousTestingTestUtils
                                            .appProperties(new String(FileUtil.readFileContents(in), StandardCharsets.UTF_8))),
                                    "application.properties");

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(CustomRoleDecoderET.class));

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

    @Test
    public void testContinuousTesting() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/servlet-secured").then()
                .statusCode(200);
        TestStatus status = utils.waitForNextCompletion();
        Assertions.assertEquals(0, status.getTotalTestsFailed());
        RestAssured.given().auth().preemptive().basic("user", "user")
                .when().get("/servlet-secured").then()
                .statusCode(200);
    }

}
