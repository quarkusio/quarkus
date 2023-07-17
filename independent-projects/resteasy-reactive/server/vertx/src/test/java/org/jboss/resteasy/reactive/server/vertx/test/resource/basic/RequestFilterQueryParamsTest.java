package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import static io.restassured.RestAssured.get;

import java.io.IOException;
import java.net.URI;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RequestFilterQueryParamsTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BlockingHelloResource.class, ReplacePathAndQueryParamsFilter.class));

    @Test
    public void test() {
        get("/dummy")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello filter"));
    }

    @Path("hello")
    public static class BlockingHelloResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@QueryParam("name") @DefaultValue("world") String name) {
            return "Hello " + name;
        }
    }

    @Provider
    @PreMatching
    public static class ReplacePathAndQueryParamsFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext)
                throws IOException {
            UriBuilder builder = requestContext.getUriInfo().getRequestUriBuilder();
            builder.replacePath("/hello");
            builder.replaceQueryParam("name", "filter");
            URI uri = builder.build();
            requestContext.setRequestUri(uri);
        }
    }
}
