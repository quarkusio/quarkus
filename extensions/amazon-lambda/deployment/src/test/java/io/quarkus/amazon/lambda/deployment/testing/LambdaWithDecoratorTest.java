package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;
import io.quarkus.test.QuarkusUnitTest;

class LambdaWithDecoratorTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(LambdaWithDecorator.class, RequestHandlerDecorator.class, InputPerson.class))
            .setLogRecordPredicate(record -> record.getLevel().intValue() == Level.INFO.intValue()
                    && record.getMessage().contains("handling request with id"))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .isNotEmpty());

    @Test
    public void testLambdaWithDecorator() throws Exception {
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

    public static class LambdaWithDecorator implements RequestHandler<InputPerson, String> {

        @Override
        public String handleRequest(InputPerson input, Context context) {
            return "Hey " + input.getName();
        }
    }

    @Priority(10)
    @Decorator
    public static class RequestHandlerDecorator<I, O> implements RequestHandler<I, O> {

        @Inject
        Logger logger;

        @Inject
        @Any
        @Delegate
        RequestHandler<I, O> delegate;

        @Override
        public O handleRequest(I i, Context context) {
            logger.info("handling request with id " + context.getAwsRequestId());
            return delegate.handleRequest(i, context);
        }
    }
}
