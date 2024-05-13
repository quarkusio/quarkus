package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class BasicAuthTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class)
                    .addAsResource("application-basic-auth.properties", "application.properties")
                    .addAsResource("test-users.properties")
                    .addAsResource("test-roles.properties"));

    @TestHTTPResource
    URI baseUri;

    @Test
    public void ensureBasicAuthSetupProperly() {
        when().get("/open").then().statusCode(200);
        when().get("/secured").then().statusCode(401);

        given().auth().preemptive().basic("foo", "bar").when().get("/open").then().statusCode(401);
        given().auth().preemptive().basic("foo", "bar").when().get("/secured").then().statusCode(401);

        given().auth().preemptive().basic("jdoe", "p4ssw0rd").when().get("/open").then().statusCode(200);
        given().auth().preemptive().basic("jdoe", "p4ssw0rd").when().get("/secured").then().statusCode(403);

        given().auth().preemptive().basic("stuart", "test").when().get("/open").then().statusCode(200);
        given().auth().preemptive().basic("stuart", "test").when().get("/secured").then().statusCode(200);
    }

    @Test
    public void testInvalidCredentials() {
        InvalidCredentials client = RestClientBuilder.newBuilder().baseUri(baseUri).build(InvalidCredentials.class);

        assertThatThrownBy(client::open).isInstanceOf(WebApplicationException.class).hasMessageContaining("401");
        assertThatThrownBy(client::secured).isInstanceOf(WebApplicationException.class).hasMessageContaining("401");
    }

    @Test
    public void testNoRoleUser() {
        NoRoleUser client = RestClientBuilder.newBuilder().baseUri(baseUri).build(NoRoleUser.class);

        assertThat(client.open()).isEqualTo("ok");
        assertThatThrownBy(client::secured).isInstanceOf(WebApplicationException.class).hasMessageContaining("403");
    }

    @Test
    public void testAdminUser() {
        AdminUser client = RestClientBuilder.newBuilder().baseUri(baseUri).build(AdminUser.class);

        assertThat(client.open()).isEqualTo("ok");
        assertThat(client.secured()).isEqualTo("ok");
    }

    @ClientBasicAuth(username = "foo", password = "bar")
    @ClientHeaderParam(name = "whatever", value = "test")
    public interface InvalidCredentials {

        @Path("open")
        @GET
        String open();

        @Path("secured")
        @GET
        String secured();
    }

    @ClientBasicAuth(username = "${norole.username}", password = "${norole.password}")
    @ClientHeaderParam(name = "whatever", value = "test")
    @ClientHeaderParam(name = "whatever2", value = "test2")
    public interface NoRoleUser {

        @Path("open")
        @GET
        String open();

        @Path("secured")
        @GET
        String secured();
    }

    @ClientBasicAuth(username = "${admin.username}", password = "${admin.password}")
    public interface AdminUser {

        @Path("open")
        @GET
        String open();

        @Path("secured")
        @GET
        String secured();
    }

    public static class Resource {
        @Route(path = "/open")
        void hello(RoutingContext context) {
            context.response().setStatusCode(200).end("ok");
        }

        @Route(path = "/secured")
        @RolesAllowed("admin")
        void secure(RoutingContext context) {
            context.response().setStatusCode(200).end("ok");
        }
    }
}
