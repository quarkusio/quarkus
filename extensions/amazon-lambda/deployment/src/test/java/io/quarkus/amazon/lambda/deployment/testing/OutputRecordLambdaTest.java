package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.test.QuarkusUnitTest;

public class OutputRecordLambdaTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(RecordReturnLambda.class, ExampleRecord.class));

    @Test
    void requestHandler_InputCollectionInputPerson_OutputCollectionOutputPerson() {

        given()
                .body("123456")
                .when()
                .post()
                .then()
                .statusCode(200)
                .body("returnValue", is(123456));
    }

    public static class RecordReturnLambda implements RequestHandler<Integer, Record> {
        @Override
        public Record handleRequest(Integer input, Context context) {
            return new ExampleRecord(input);
        }
    }

    public record ExampleRecord(int returnValue) {
    }
}
