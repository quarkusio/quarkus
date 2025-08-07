package io.quarkus.amazon.lambda.deployment.testing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.test.QuarkusUnitTest;

class LambdaWithHierarchyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(GreetingLambda.class, AbstractRequestHandler.class, Person.class));

    // https://github.com/quarkusio/quarkus/issues/49413
    @Test
    public void testLambdaWithHierarchy() throws Exception {
        // you test your lambdas by invoking on http://localhost:8081
        // this works in dev mode too

        Person in = new Person("dagrammy");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(in)
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Hello dagrammy"));
    }

    @ApplicationScoped
    @Named("GreetingLambda")
    public static class GreetingLambda extends AbstractRequestHandler<Person, String> {

        public String getName(Person input) {
            return "Hello " + input.getName();
        }

    }

    public static class Person {

        private final String name;

        public Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static abstract class AbstractRequestHandler<T, R> implements RequestHandler<T, R> {

        @Override
        public R handleRequest(T input, Context context) {
            return getName(input);
        }

        public abstract R getName(T input);
    }
}
