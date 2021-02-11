package io.quarkus.undertow.test;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AnnotatedServletInitParamsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AnnotatedServletInitParam.class, AnnotatedFilterInitParam.class));

    @Test
    public void testAnnotatedInitParamsServlet() {
        RestAssured.when().get(AnnotatedServletInitParam.SERVLET_ENDPOINT).then()
                .statusCode(200)
                .body(CoreMatchers.is("invoked-before-chain\n" +
                        "AnnotatedInitFilterParamValue\n" +
                        "AnnotatedInitParamValue\n" +
                        "invoked-after-chain\n"));
    }
}
