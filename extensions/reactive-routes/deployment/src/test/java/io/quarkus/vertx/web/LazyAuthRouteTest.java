package io.quarkus.vertx.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.RoutingContext;

public class LazyAuthRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n"), "application.properties")
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, HelloWorldBean.class));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testAuthZInPlace() {
        given().auth().basic("user", "user").when().get("/hello-ra").then().statusCode(403);
    }

    @Test
    public void testRolesAllowedVoidMethod() {
        given().auth().basic("admin", "admin").when().get("/hello-ra").then().statusCode(200).body(is("Hello admin"));
    }

    @Test
    public void testRolesAllowedDirectResponse() {
        given().auth().basic("admin", "admin").when().get("/hello-ra-direct").then().statusCode(200).body(is("Hello admin"));
    }

    @Test
    public void testAuthenticated() {
        given().auth().basic("user", "user").when().get("/hello-auth").then().statusCode(200);
    }

    @Test
    public void testDenyAll() {
        given().auth().basic("user", "user").when().get("/hello-deny").then().statusCode(403);
    }

    public static final class HelloWorldBean {

        @Inject
        SecurityIdentityAssociation securityIdentityAssociation;

        @Authenticated
        @Route(path = "/hello-auth", methods = Route.HttpMethod.GET)
        public void greetingsAuth(RoutingContext rc) {
            respond(rc);
        }

        @RolesAllowed("admin")
        @Route(path = "/hello-ra", methods = Route.HttpMethod.GET)
        public void greetingsRA(RoutingContext rc) {
            respond(rc);
        }

        @RolesAllowed("admin")
        @Route(path = "/hello-ra-direct", methods = Route.HttpMethod.GET)
        public String greetingsRADirect() {
            return hello();
        }

        @DenyAll
        @Route(path = "/hello-deny", methods = Route.HttpMethod.GET)
        public String greetingsDeny() {
            return hello();
        }

        private void respond(RoutingContext rc) {
            rc.response().end(hello());
        }

        private String hello() {
            return "Hello " + securityIdentityAssociation.getIdentity().getPrincipal().getName();
        }
    }

}
