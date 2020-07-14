package io.quarkus.qrs.test;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SimpleQrsTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SimpleQrsResource.class, Person.class);
                }
            });

    @Test
    public void simpleTest() {
        RestAssured.get("/simple/foo")
                .then().body(Matchers.equalTo("GET:foo"));
        RestAssured.get("/simple")
                .then().body(Matchers.equalTo("GET"));

        RestAssured.post("/simple")
                .then().body(Matchers.equalTo("POST"));
    }

    @Test
    public void testJson() {
        RestAssured.get("/simple/person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));
    }
    @Test
    public void testBlocking() {
        RestAssured.get("/simple/blocking")
                .then().body(Matchers.equalTo("true"));
    }
}
