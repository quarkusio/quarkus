package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class StringShapedDateWithTimestampsEnabledTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(StringShapedDateResource.class,
                                    StringShapedDateBean.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.jackson.write-dates-as-timestamps=true\n" +
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testStringShapedDateWithTimestampsEnabled() {
        RestAssured.get("/string-shaped-date")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("string-shaped-date"))
                .body("date", Matchers.isA(String.class));
    }
}
