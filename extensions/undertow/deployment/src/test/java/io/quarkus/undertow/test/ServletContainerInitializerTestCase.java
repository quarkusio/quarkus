package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.containsString;

import jakarta.servlet.ServletContainerInitializer;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ServletContainerInitializerTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsServiceProvider(ServletContainerInitializer.class, TestSCI.class)
                    .addAsResource(new StringAsset("index.html"), "META-INF/resources/index.html")
                    .addClasses(SCIInterface.class, SCIImplementation.class, TestSCI.class, SCIAnnotation.class,
                            AnnotatedSCIClass.class));

    @Test
    public void testSci() {
        RestAssured.when().get("/sci").then()
                .statusCode(200)
                .body(containsString("io.quarkus.undertow.test.SCIImplementation"),
                        containsString("io.quarkus.undertow.test.AnnotatedSCIClass"));
    }

}