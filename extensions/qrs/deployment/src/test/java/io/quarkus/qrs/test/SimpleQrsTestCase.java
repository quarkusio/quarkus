package io.quarkus.qrs.test;

import java.util.List;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class SimpleQrsTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SimpleQrsResource.class, Person.class, TestRequestFilter.class,
                                    TestResponseFilter.class, TestException.class, TestExceptionMapper.class);
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

        RestAssured.delete("/simple")
                .then().body(Matchers.equalTo("DELETE"));

        RestAssured.put("/simple")
                .then().body(Matchers.equalTo("PUT"));

        RestAssured.head("/simple")
                .then().header("Stef", "head");

        RestAssured.options("/simple")
                .then().body(Matchers.equalTo("OPTIONS"));

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

    @Test
    public void testFilters() {
        Headers headers = RestAssured.get("/simple/filters")
                .then().extract().headers();
        List<String> filters = headers.getValues("filter");
        Assertions.assertEquals(2, filters.size());
        Assertions.assertTrue(filters.contains("request"));
        Assertions.assertTrue(filters.contains("response"));
    }

    @Test
    public void testException() {
        RestAssured.get("/simple/mapped-exception")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
        RestAssured.get("/simple/unknown-exception")
                .then().statusCode(500);
        RestAssured.get("/simple/web-application-exception")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
    }
}
