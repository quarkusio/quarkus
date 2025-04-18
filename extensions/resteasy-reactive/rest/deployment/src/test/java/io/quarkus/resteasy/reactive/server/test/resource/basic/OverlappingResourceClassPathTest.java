package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

class OverlappingResourceClassPathTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClasses(UsersResource.class);
                    war.addClasses(UserResource.class);
                    war.addClasses(GreetingResource.class);
                    return war;
                }
            });

    @Test
    void basicTest() {
        given()
                .get("/users/userId")
                .then()
                .statusCode(200)
                .body(equalTo("userId"));

        given()
                .get("/users/userId/by-id")
                .then()
                .statusCode(200)
                .body(equalTo("getByIdInUserResource-userId"));

        // test that only the User, and UsersResource have matched, and that the initial matches are sorted by remaining length
        given()
                .get("/users/userId/resource-does-not-exist")
                .then()
                .statusCode(404)
                .body(equalTo("/resource-does-not-exist|/userId/resource-does-not-exist|"));
    }

    @Path("/users")
    public static class UsersResource {

        @GET
        @Path("{id}")
        public String getByIdInUsersResource(@RestPath String id) {
            return id;
        }
    }

    @Path("/users/{id}")
    public static class UserResource {

        @GET
        @Path("by-id")
        public String getByIdInUserResource(@RestPath String id) {
            return "getByIdInUserResource-" + id;
        }
    }

    @Path("/i-do-not-match")
    public static class GreetingResource {

        @GET
        @Path("greet")
        public String greet() {
            return "Hello";
        }
    }

    @ServerExceptionMapper
    public RestResponse<String> handle(RuntimeException ignored, ServerRequestContext requestContext) {
        ResteasyReactiveRequestContext ctxt = (ResteasyReactiveRequestContext) ClientProxy.unwrap(requestContext);
        String remainings = "";
        for (RequestMapper.RequestMatch<RestInitialHandler.InitialMatch> initialMatchRequestMatch : ctxt.getInitialMatches()) {
            remainings += initialMatchRequestMatch.remaining + "|";
        }
        return RestResponse.status(Response.Status.NOT_FOUND, remainings);
    }
}
