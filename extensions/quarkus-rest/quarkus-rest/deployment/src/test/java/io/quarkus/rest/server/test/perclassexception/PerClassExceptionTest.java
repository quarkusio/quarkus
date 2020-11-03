package io.quarkus.rest.server.test.perclassexception;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PerClassExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FirstResource.class, SecondResource.class,
                                    MyException.class);
                }
            });

    @Test
    public void testResourceWithExceptionMapper() {
        RestAssured.get("/first?name=IllegalState")
                .then().statusCode(409);
        RestAssured.get("/first?name=IllegalArgument")
                .then().statusCode(409);
        RestAssured.get("/first?name=My")
                .then().statusCode(410).body(Matchers.equalTo("/first->throwsVariousExceptions"));
        RestAssured.get("/first?name=Other")
                .then().statusCode(500);
    }

    @Test
    public void testResourceWithoutExceptionMapper() {
        RestAssured.get("/second?name=IllegalState")
                .then().statusCode(500);
        RestAssured.get("/second?name=IllegalArgument")
                .then().statusCode(500);
        RestAssured.get("/second?name=My")
                .then().statusCode(500);
        RestAssured.get("/second?name=Other")
                .then().statusCode(500);
    }
}
