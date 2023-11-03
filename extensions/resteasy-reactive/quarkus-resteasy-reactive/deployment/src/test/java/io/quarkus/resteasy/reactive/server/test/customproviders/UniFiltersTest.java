package io.quarkus.resteasy.reactive.server.test.customproviders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class UniFiltersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(UniVoidRequestFilter.class, UniResponseRequestFilter.class, UniResponseFilter.class,
                                    UniException.class, UniExceptionMapper.class,
                                    UniFiltersResource.class);
                }
            });

    @Test
    public void testUniVoid() {
        Headers headers = RestAssured.given().header("some-uni-input", "bar").get("/uni/req")
                .then().statusCode(200).body(Matchers.containsString("/uni/req-bar")).extract().headers();
        assertThat(headers.getValues("java-method")).containsOnly("filters");
    }

    @Test
    public void testUniVoidThrowingException() {
        RestAssured.given().header("some-uni-exception-input", "whatever").get("/uni/req")
                .then().statusCode(202).body(Matchers.containsString("whatever"));
    }

    @Test
    public void testUniResponseReturningNull() {
        Headers headers = RestAssured.given().header("some-other-uni-input", "bar").get("/uni/req")
                .then().statusCode(200).body(Matchers.containsString("/uni/req-bar")).extract().headers();
        assertThat(headers.getValues("java-method")).containsOnly("filters");
    }

    @Test
    public void testUniVoidReturningResponse() {
        RestAssured.given().header("some-other-uni-exception-input", "whatever").get("/uni/req")
                .then().statusCode(500).body(Matchers.containsString("whatever"));
    }
}
