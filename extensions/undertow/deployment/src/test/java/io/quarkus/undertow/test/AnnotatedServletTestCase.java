package io.quarkus.undertow.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 *
 */
public class AnnotatedServletTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AnnotatedServlet.class));

    /**
     * Tests the issue noted in https://github.com/quarkusio/quarkus/issues/5293
     * where {@code UndertowBuildStep} would throw an NPE when the annotated servlet
     * had additional annotation(s) but not the {@code @ServletSecurity} annotation
     *
     * @throws Exception
     */
    @Test
    public void testNPE() throws Exception {
        when().get(AnnotatedServlet.SERVLET_ENDPOINT).then()
                .statusCode(200)
                .body(is(AnnotatedServlet.OK_RESPONSE));
    }
}
