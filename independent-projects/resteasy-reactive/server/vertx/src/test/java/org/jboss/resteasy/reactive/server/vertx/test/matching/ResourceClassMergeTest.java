package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ResourceClassMergeTest {
    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(MatchDefaultRegexDifferentNameResourceA.class,
                    MatchDefaultRegexDifferentNameResourceB.class, MatchCustomRegexDifferentNameResourceA.class,
                    MatchCustomRegexDifferentNameResourceB.class);
        }
    });

    @Test
    public void testCallMatchDefaultRegexDifferentNameResource() {
        given().when().get("routing-broken/abc/some/other/path").then().statusCode(200).body(is("abc"));

        given().when().get("routing-broken/efg/some/path").then().statusCode(200).body(is("efg"));
    }

    @Test
    public void testCallMatchCustomRegexDifferentNameResource() {
        given().when().get("routing-broken-custom-regex/abc/some/other/path").then().statusCode(200).body(is("abc"));

        given().when().get("routing-broken-custom-regex/efg/some/path").then().statusCode(200).body(is("efg"));
    }

    @Path("/routing-broken/{id1}")
    public static class MatchDefaultRegexDifferentNameResourceA {
        @GET
        @Path("/some/other/path")
        @Produces(MediaType.TEXT_PLAIN)
        public Response doSomething(@PathParam("id1") String id) {
            return Response.ok(id).build();
        }
    }

    @Path("/routing-broken/{id}")
    public static class MatchDefaultRegexDifferentNameResourceB {
        @GET
        @Path("/some/path")
        @Produces(MediaType.TEXT_PLAIN)
        public Response doSomething(@PathParam("id") String id) {
            return Response.ok(id).build();
        }
    }

    @Path("/routing-broken-custom-regex/{id1: [a-zA-Z]+}")
    public static class MatchCustomRegexDifferentNameResourceA {
        @GET
        @Path("/some/other/path")
        @Produces(MediaType.TEXT_PLAIN)
        public Response doSomething(@PathParam("id1") String id) {
            return Response.ok(id).build();
        }
    }

    @Path("/routing-broken-custom-regex/{id: [a-zA-Z]+}")
    public static class MatchCustomRegexDifferentNameResourceB {
        @GET
        @Path("/some/path")
        @Produces(MediaType.TEXT_PLAIN)
        public Response doSomething(@PathParam("id") String id) {
            return Response.ok(id).build();
        }
    }
}
