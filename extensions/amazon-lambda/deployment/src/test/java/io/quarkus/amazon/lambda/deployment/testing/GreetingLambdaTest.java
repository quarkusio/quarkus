package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;
import io.quarkus.test.QuarkusUnitTest;

class GreetingLambdaTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(GreetingLambda.class, InputPerson.class));

    @Test
    public void requestHandler_InputPerson_OutputString() throws Exception {
        // you test your lambdas by invoking on http://localhost:8081
        // this works in dev mode too

        InputPerson in = new InputPerson("Stu");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hey Stu"));
    }

}
