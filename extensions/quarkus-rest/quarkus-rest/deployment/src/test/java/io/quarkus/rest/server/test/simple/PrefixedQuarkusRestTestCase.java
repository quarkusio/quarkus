package io.quarkus.rest.server.test.simple;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * This test makes sure that the http.root-path is honored by quarkus-rest
 * by running the same test as SimpleQuarkusRestTestCase
 */
public class PrefixedQuarkusRestTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.root-path", "/prefix")
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SimpleQuarkusRestResource.class, Person.class,
                                    TestRequestFilter.class, TestRequestFilterWithHighPriority.class,
                                    TestRequestFilterWithHighestPriority.class,
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
                                    TestWriter.class, TestClass.class);
                }
            });

    @Test
    public void simpleTest() {
        Assertions.assertEquals("prefix", RestAssured.basePath);
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
}
