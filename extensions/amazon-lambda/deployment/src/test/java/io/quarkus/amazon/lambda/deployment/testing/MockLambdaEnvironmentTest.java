package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.test.QuarkusExtensionTest;

class MockLambdaEnvironmentTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(ContextEchoLambda.class, InputPerson.class))
            .overrideRuntimeConfigKey(AmazonLambdaApi.INTERNAL_FUNCTION_NAME, "TestFunction");

    @Test
    void contextExposesMockFunctionName() {
        InputPerson in = new InputPerson("Stu");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hey Stu from TestFunction"));
    }
}
