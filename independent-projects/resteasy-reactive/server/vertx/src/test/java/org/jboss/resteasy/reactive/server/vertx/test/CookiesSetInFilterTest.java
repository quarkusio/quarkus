package org.jboss.resteasy.reactive.server.vertx.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;

import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.common.headers.NewCookieHeaderDelegate;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CookiesSetInFilterTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClasses(TestResource.class, Filters.class));

    @Test
    void requestDoesNotContainCookie() {
        when().get("/test").then().statusCode(200).body(is("foo"));
    }

    @Test
    void requestContainsCookie() {
        given().cookie("dummy", "bar").when().get("/test").then().statusCode(200).body(is("bar"));
    }

    @Path("test")
    public static class TestResource {

        @GET
        public String get(@RestCookie String dummy) {
            return dummy;
        }
    }

    public static class Filters {

        @ServerRequestFilter
        public void setCookieIfMissing(ContainerRequestContext context) {
            if (!context.getCookies().containsKey("dummy")) {
                context.getHeaders().add(HttpHeaders.COOKIE,
                        NewCookieHeaderDelegate.INSTANCE.toString(new NewCookie.Builder("dummy").value("foo").build()));
            }
        }
    }
}
