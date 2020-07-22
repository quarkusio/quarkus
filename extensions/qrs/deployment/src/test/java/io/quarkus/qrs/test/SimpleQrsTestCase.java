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

        RestAssured.get("/missing")
        .then().statusCode(404);

        RestAssured.post("/missing")
        .then().statusCode(404);

        RestAssured.delete("/missing")
        .then().statusCode(404);

        System.err.println("DEL");
        RestAssured.delete("/simple")
            .then().body(Matchers.equalTo("DELETE"));

        System.err.println("PUT");
        RestAssured.put("/simple")
            .then().body(Matchers.equalTo("PUT"));

        System.err.println("HEAD");
        RestAssured.head("/simple")
            .then().header("Stef", "head");

        System.err.println("OPTIONS");
        RestAssured.options("/simple")
        .then().body(Matchers.equalTo("OPTIONS"));

        System.err.println("PATCH");
        RestAssured.patch("/simple")
        .then().body(Matchers.equalTo("PATCH"));
    }

    @Test
    public void testParams() {
        RestAssured.with()
            .queryParam("q", "qv")
            .header("h", "hv")
            .formParam("f", "fv")
            .post("/simple/params/pv")
            .then().body(Matchers.equalTo("params: p: pv, q: qv, h: hv, f: fv"));
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
