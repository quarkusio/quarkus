package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.PathSegment;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PathSegmentTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClass(Resource.class);
        }
    });

    @Test
    public void testRegularMatch() {
        given().when().get("/test/list/{seg1}/{seg2}", "key1", "key2").then().statusCode(200)
                .body(is("Path(2): [key1, key2]"));
    }

    @Test
    public void testEncodedPath() {
        given().when().get("/test/list/{seg1}/{seg2}", "key/1", "key/2").then().statusCode(200)
                .body(is("Path(2): [key/1, key/2]"));
    }

    @Path("/test")
    public static class Resource {
        @GET
        @Path("/list/{primaryKey: .+}")
        public String pathAsList(@PathParam("primaryKey") List<PathSegment> path) {
            return String.format("Path(%d): %s", path.size(), path);
        }
    }
}
