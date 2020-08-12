package io.quarkus.qrs.test.simple;

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
                                    TestResponseFilter.class, HelloService.class, TestException.class,
                                    TestExceptionMapper.class, TestPreMatchRequestFilter.class,
                                    SubResource.class,
                                    TestWriter.class, TestClass.class);
                }
            });

    @Test
    public void simpleTest() {
        RestAssured.get("/simple")
                .then().body(Matchers.equalTo("GET"));
        RestAssured.get("/simple/foo")
                .then().body(Matchers.equalTo("GET:foo"));

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
    public void testInjection() {
        RestAssured.get("/simple/hello")
                .then().body(Matchers.equalTo("Hello"));
    }

    @Test
    public void testSubResource() {
        RestAssured.get("/simple/sub/otherSub")
                .then().body(Matchers.equalTo("otherSub"));
        RestAssured.get("/simple/sub")
                .then().body(Matchers.equalTo("sub"));
    }

    @Test
    public void testParams() {
        RestAssured.with()
                .queryParam("q", "qv")
                .header("h", "123")
                .formParam("f", "fv")
                .post("/simple/params/pv")
                .then().body(Matchers.equalTo("params: p: pv, q: qv, h: 123, f: fv"));
    }

    @Test
    public void testJson() {
        RestAssured.get("/simple/person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        RestAssured.with().body(person).post("/simple/person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));
    }

    @Test
    public void testBlocking() {
        RestAssured.get("/simple/blocking")
                .then().body(Matchers.equalTo("true"));
    }

    @Test
    public void testPreMatchFilter() {
        RestAssured.get("/simple/pre-match")
                .then().body(Matchers.equalTo("pre-match-post"));
        RestAssured.post("/simple/pre-match")
                .then().body(Matchers.equalTo("pre-match-post"));
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

    @Test
    public void testWriter() {
        RestAssured.get("/simple/lookup-writer")
                .then().body(Matchers.equalTo("OK"));
        RestAssured.get("/simple/writer")
                .then().body(Matchers.equalTo("WRITER"));

        RestAssured.get("/simple/fast-writer")
                .then().body(Matchers.equalTo("OK"));

        RestAssured.get("/simple/writer/vertx-buffer")
                .then().body(Matchers.equalTo("VERTX-BUFFER"));
    }

    @Test
    public void testAsync() {
        RestAssured.get("/simple/async/cs/ok")
                .then().body(Matchers.equalTo("CS-OK"));
        RestAssured.get("/simple/async/cs/fail")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
        RestAssured.get("/simple/async/uni/ok")
                .then().body(Matchers.equalTo("UNI-OK"));
        RestAssured.get("/simple/async/uni/fail")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
    }
}
