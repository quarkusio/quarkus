package io.quarkus.agroal.test;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class AgroalDevModeTestCase {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(DevModeResource.class)
                            .add(new StringAsset("quarkus.datasource.db-kind=h2\n" +
                                    "quarkus.datasource.username=USERNAME-NAMED\n" +
                                    "quarkus.datasource.jdbc.url=jdbc:h2:tcp://localhost/mem:testing\n" +
                                    "quarkus.datasource.jdbc.driver=org.h2.Driver\n"), "application.properties");
                }
            });

    @Test
    public void testAgroalHotReplacement() {
        RestAssured
                .get("/dev/user")
                .then()
                .body(Matchers.equalTo("USERNAME-NAMED"));
        test.modifyResourceFile("application.properties", s -> s.replace("USERNAME-NAMED", "OTHER-USER"));

        RestAssured
                .get("/dev/user")
                .then()
                .body(Matchers.equalTo("OTHER-USER"));
    }
}
