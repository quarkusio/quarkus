package io.quarkus.rest.test.simple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class SimpleQuarkusRestTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TRACE.class, SimpleQuarkusRestResource.class, Person.class,
                                    TestRequestFilter.class, TestRequestFilterWithHighPriority.class,
                                    TestRequestFilterWithHighestPriority.class, ResourceInfoInjectingFilter.class,
                                    Foo.class, Bar.class,
                                    TestFooRequestFilter.class, TestBarRequestFilter.class, TestFooBarRequestFilter.class,
                                    TestFooResponseFilter.class, TestBarResponseFilter.class, TestFooBarResponseFilter.class,
                                    TestResponseFilter.class, HelloService.class, TestException.class,
                                    TestExceptionMapper.class, TestPreMatchRequestFilter.class,
                                    FeatureMappedException.class, FeatureMappedExceptionMapper.class,
                                    FeatureRequestFilterWithNormalPriority.class, FeatureRequestFilterWithHighestPriority.class,
                                    FeatureResponseFilter.class, DynamicFeatureRequestFilterWithLowPriority.class,
                                    TestFeature.class, TestDynamicFeature.class,
                                    SubResource.class, RootAResource.class, RootBResource.class,
                                    QueryParamResource.class, HeaderParamResource.class,
                                    TestWriter.class, TestClass.class,
                                    SimpleBeanParam.class, OtherBeanParam.class, FieldInjectedResource.class);
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
    public void test405() {
        RestAssured.put("/ctor-query")
                .then().statusCode(405);

        RestAssured.put("/simple/person")
                .then().statusCode(405);
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
        RestAssured.with().body(person).contentType("application/json; charset=utf-8").post("/simple/person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));
    }

    @Test
    public void testLargeJsonPost() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; ++i) {
            sb.append("abc");
        }
        String longString = sb.toString();
        Person person = new Person();
        person.setFirst(longString);
        person.setLast(longString);
        RestAssured.with().body(person).contentType("application/json; charset=utf-8").post("/simple/person-large")
                .then().body("first", Matchers.equalTo(longString)).body("last", Matchers.equalTo(longString));
    }

    @Test
    public void testAsyncJson() {
        RestAssured.get("/simple/async-person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));
    }

    @Test
    public void testValidatedJson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        RestAssured.with().body(person).contentType("application/json").post("/simple/person-validated")
                .then().statusCode(200).body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured.with().body(person).contentType("application/json").post("/simple/person-invalid-result")
                .then()
                .statusCode(500)
                .contentType("application/json");

        person.setLast(null);
        RestAssured.with().body(person).contentType("application/json").post("/simple/person-validated")
                .then()
                .statusCode(400)
                .contentType("application/json");
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
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-default");
        assertThat(headers.getValues("filter-response")).containsOnly("default");

        headers = RestAssured.get("/simple/fooFilters")
                .then().extract().headers();
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-foo-default");
        assertThat(headers.getValues("filter-response")).containsOnly("default-foo");

        headers = RestAssured.get("/simple/barFilters")
                .then().extract().headers();
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-default-bar");
        assertThat(headers.getValues("filter-response")).containsOnly("default-bar");

        headers = RestAssured.get("/simple/fooBarFilters")
                .then().extract().headers();
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-foo-default-bar-foobar");
        assertThat(headers.getValues("filter-response")).containsOnly("default-foo-bar-foobar");
    }

    @Test
    public void testProviders() {
        RestAssured.get("/simple/providers")
                .then().body(Matchers.containsString("TestException"))
                .statusCode(200);
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

    @Test
    public void testMultiResourceSamePath() {
        RestAssured.get("/a")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("a"));
        RestAssured.get("/b")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("b"));
    }

    @Test
    public void testRequestAndResponseParams() {
        RestAssured.get("/simple/request-response-params")
                .then()
                .body(Matchers.equalTo("127.0.0.1"))
                .header("dummy", "value");

    }

    @Test
    public void testJaxRsRequest() {
        RestAssured.get("/simple/jax-rs-request")
                .then()
                .body(Matchers.equalTo("GET"));
    }

    @Test
    public void testFeature() {
        RestAssured.get("/simple/feature-mapped-exception")
                .then()
                .statusCode(667);

        Headers headers = RestAssured.get("/simple/feature-filters")
                .then().extract().headers();
        assertThat(headers.getValues("feature-filter-request")).containsOnly("authentication-default");
        assertThat(headers.getValues("feature-filter-response")).containsExactly("high-priority", "normal-priority");
    }

    @Test
    public void testDynamicFeature() {
        Headers headers = RestAssured.get("/simple/dynamic-feature-filters")
                .then().extract().headers();
        assertThat(headers.getValues("feature-filter-request")).containsOnly("authentication-default-low");
        assertThat(headers.getValues("feature-filter-response")).containsExactly("high-priority", "normal-priority",
                "low-priority");
    }

    @Test
    public void testResourceInfo() {
        Headers headers = RestAssured.get("/simple/resource-info")
                .then().extract().headers();
        assertThat(headers.getValues("class-name")).containsOnly("SimpleQuarkusRestResource");
        assertThat(headers.getValues("method-name")).containsOnly("resourceInfo");
    }

    @Test
    public void testQueryParamInCtor() {
        RestAssured.get("/ctor-query")
                .then().body(Matchers.is(emptyString()));

        RestAssured.get("/ctor-query?q1=v1")
                .then().body(Matchers.equalTo("v1"));

        RestAssured.get("/ctor-query?q1=v11")
                .then().body(Matchers.equalTo("v11"));

        RestAssured.get("/ctor-query?q2=v2")
                .then().body(Matchers.is(emptyString()));
    }

    @Test
    public void testHeaderParamInCtor() {
        RestAssured.get("/ctor-header")
                .then().body(Matchers.is(emptyString()));

        RestAssured.with().header("h1", "v1").get("/ctor-header")
                .then().body(Matchers.equalTo("v1"));

        RestAssured.with().header("h1", "v11").get("/ctor-header")
                .then().body(Matchers.equalTo("v11"));

        RestAssured.with().header("h2", "v2").get("/ctor-header")
                .then().body(Matchers.is(emptyString()));
    }

    @Test
    public void testFormMap() {
        RestAssured
                .given()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/x-www-form-urlencoded")
                .formParam("f1", "v1")
                .formParam("f2", "v2")
                .post("/simple/form-map")
                .then()
                .contentType("application/x-www-form-urlencoded")
                .body(Matchers.equalTo("f1=v1&f2=v2"));
    }

    @Test
    public void testJsonp() {
        RestAssured.with().body("{\"k\": \"v\"}").contentType("application/json; charset=utf-8").post("/simple/jsonp-object")
                .then().statusCode(200).body(Matchers.equalTo("v"));

        RestAssured.with().body("[{}, {}]").contentType("application/json").post("/simple/jsonp-array")
                .then().statusCode(200).body(Matchers.equalTo("2"));
    }

    @Test
    public void testPrimitiveBody() {
        RestAssured.with().body("true").contentType("text/plain").post("/simple/bool")
                .then().statusCode(200).contentType("text/plain").body(Matchers.equalTo("true"));
    }

    @Test
    public void testCustomHttpMethodAnnotation() {
        RestAssured.request("TRACE", "/simple/trace")
                .then().statusCode(200);
    }

    @Test
    public void simpleFieldInjection() {
        RestAssured
                .with()
                .header("header", "one-header")
                .queryParam("query", "one-query")
                .get("/injection/field")
                .then().body(Matchers.equalTo("query=one-query, header=one-header, uriInfo.path=/injection/field, "
                        + "beanParam.query=one-query, beanParam.header=one-header, beanParam.uriInfo.path=/injection/field, "
                        + "beanParam.otherBeanParam.query=one-query, beanParam.otherBeanParam.header=one-header, beanParam.otherBeanParam.uriInfo.path=/injection/field"));
        RestAssured
                .with()
                .header("header", "one-header")
                .queryParam("query", "one-query")
                .get("/injection/param")
                .then().body(Matchers.equalTo("query=one-query, header=one-header, uriInfo.path=/injection/param, "
                        + "beanParam.query=one-query, beanParam.header=one-header, beanParam.uriInfo.path=/injection/param, "
                        + "beanParam.otherBeanParam.query=one-query, beanParam.otherBeanParam.header=one-header, beanParam.otherBeanParam.uriInfo.path=/injection/param"));
    }
}
