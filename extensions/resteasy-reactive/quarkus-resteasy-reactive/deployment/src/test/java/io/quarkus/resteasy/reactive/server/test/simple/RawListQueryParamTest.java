package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RawListQueryParamTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloResource.class));

    @Test
    public void noQueryParams() {
        RestAssured.get("/hello")
                .then().statusCode(200).body(Matchers.equalTo("hello world"));
    }

    @Test
    public void singleQueryParam() {
        RestAssured.get("/hello?name=foo")
                .then().statusCode(200).body(Matchers.equalTo("hello foo"));
    }

    @Test
    public void multipleQueryParams() {
        RestAssured.get("/hello?name=foo&name=bar")
                .then().statusCode(200).body(Matchers.equalTo("hello foo,bar"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public String hello(@RestQuery("name") List names) {
            if (names.isEmpty()) {
                return "hello world";
            }
            return "hello " + String.join(",", names);
        }

    }
}
