package io.quarkus.vertx.web.params;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Header;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class RouteMethodParametersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class));

    @Test
    public void testRoutes() {
        //RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        when().get("/hello").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-response").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-rx-response").then().statusCode(200).body(is("Hello world!"));
        when().get("/hello-response-nonvoid?name=foo").then().statusCode(200).body(is("Hello foo!"));
        when().get("/hello-all").then().statusCode(200).body(is("ok"));
        when().get("/hello-params?name=foo&identifier=10").then().statusCode(200).body(is("Hello foo! Your id is 10"));
        when().get("/hello/42?name=foo&identifier=10").then().statusCode(200)
                .body(is("Hello 42! Your name is foo and id is 10"));
        when().get("/hello-multiple-params?id=foo&id=10").then().statusCode(200)
                .body(is("foo,10"));
        given().header("My-Header", "fooooo").get("/hello-header").then().statusCode(200).body(is("fooooo"));
        given().header("My-Header", "fooooo").header("My-Header", "baaar").get("/hello-multiple-headers").then().statusCode(200)
                .body(is("fooooo,baaar"));
        given().contentType("application/json").body("{\"name\":\"Eleven\"}")
                .post("/hello-body").then().statusCode(200).body("name", is("Eleven"))
                .body("id", is(11));
        given().contentType("application/json").body("{\"name\":\"Eleven\"}")
                .post("/hello-body-as-string").then().statusCode(200).body("NAME", is("ELEVEN"));
        given().contentType("application/json").body("{\"name\":\"Eleven\"}")
                .post("/hello-body-json-object").then().statusCode(200).body("name", is("Eleven"))
                .body("id", is(11));
        given().contentType("application/json").body("[11]")
                .post("/hello-body-json-array").then().statusCode(200).body(is("[11,12]"));
        given().contentType("application/json").body("{\"name\":\"Eleven\"}")
                .post("/hello-body-pojo").then().statusCode(200).body("name", is("Eleven"))
                .body("id", is(11));
        given().contentType("application/json").body("{\"name\":\"Eleven\"}")
                .post("/hello-body-pojo?id=13").then().statusCode(200).body("name", is("Eleven"))
                .body("id", is(13));
    }

    static class SimpleBean {

        @Route(path = "/hello")
        void hello1(HttpServerRequest request, HttpServerResponse response) {
            String name = request.getParam("name");
            response.setStatusCode(200).end("Hello " + (name != null ? name : "world") + "!");
        }

        @Route(path = "/hello-response")
        void hello2(HttpServerResponse response) {
            response.setStatusCode(200).end("Hello world!");
        }

        @Route(path = "/hello-rx-response")
        void hello3(io.vertx.reactivex.core.http.HttpServerResponse response) {
            response.setStatusCode(200).end("Hello world!");
        }

        @Route(path = "/hello-response-nonvoid")
        String hello4(HttpServerRequest request) {
            return "Hello " + request.getParam("name") + "!";
        }

        @Route(path = "/hello-all")
        String hello5(io.vertx.reactivex.core.http.HttpServerResponse rxResponse, RoutingContext routingContext,
                RoutingExchange routingExchange, HttpServerRequest request, HttpServerResponse response,
                io.vertx.reactivex.core.http.HttpServerRequest rxRequest) {
            assertNotNull(rxRequest);
            assertNotNull(rxResponse);
            assertNotNull(routingContext);
            assertNotNull(routingExchange);
            assertNotNull(request);
            assertNotNull(response);
            return "ok";
        }

        @Route
        String helloParams(@Param Optional<String> name, @Param("identifier") String id) {
            return "Hello " + name.orElse("world") + "! Your id is " + id;
        }

        @Route(path = "hello/:id")
        String helloPathParam(@Param String id, @Param String name, @Param String identifier) {
            return "Hello " + id + "! Your name is " + name + " and id is " + identifier;
        }

        @Route
        String helloMultipleParams(@Param("id") List<String> ids) {
            return ids.stream().collect(Collectors.joining(","));
        }

        @Route
        String helloHeader(@Header("My-Header") String myHeader, @Header Optional<String> missingHeader) {
            assertFalse(missingHeader.isPresent());
            return myHeader;
        }

        @Route
        String helloMultipleHeaders(@Header("My-Header") List<String> headers) {
            return headers.stream().collect(Collectors.joining(","));
        }

        @Route(produces = "application/json")
        JsonObject helloBody(@Body Buffer body) {
            return body.toJsonObject().put("id", 11);
        }

        @Route(produces = "application/json")
        String helloBodyAsString(@Body String body) {
            return body.toUpperCase();
        }

        @Route(produces = "application/json")
        JsonObject helloBodyJsonObject(@Body JsonObject body) {
            return body.put("id", 11);
        }

        @Route(produces = "application/json")
        JsonArray helloBodyJsonArray(@Body JsonArray body) {
            return body.add(12);
        }

        @Route(produces = "application/json")
        Person helloBodyPojo(@Body Person person, @Param("id") Optional<String> primaryKey) {
            person.setId(primaryKey.map(Integer::valueOf).orElse(11));
            return person;
        }

    }

    public static class Person {

        private String name;
        private int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

}
