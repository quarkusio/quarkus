package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SubresourceCustomRegexTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(RootResource.class)
                            .addClass(SubResource.class);
                }
            });

    @Test
    public void testRequestForwardedToSubresource() {
        given()
                .when().get("/TomOther/greet")
                .then()
                .statusCode(200)
                .body(is("Hello Tom"));
        given()
                .when().get("/TimOther/greet")
                .then()
                .statusCode(200)
                .body(is("Hello Tim"));
        given()
                .when().get("/Tom/greet")
                .then()
                .statusCode(404);
    }

    @Path("/")
    public static class RootResource {

        @Path("{id}Other")
        public SubResource sub() {
            return new SubResource();
        }
    }

    @Produces("text/plain")
    public static class SubResource {

        @Path("/greet")
        @GET
        public String greet(@PathParam("id") String id) {
            return "Hello " + id;
        }

    }

}
