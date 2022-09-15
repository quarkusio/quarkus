package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.restassured.RestAssured;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RawListQueryParamTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
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
