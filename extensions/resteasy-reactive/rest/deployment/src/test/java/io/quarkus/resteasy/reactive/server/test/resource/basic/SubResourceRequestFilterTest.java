package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SubResourceRequestFilterTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(RestResource.class, RestSubResource.class, SingleExecutionFilter.class,
                            MiddleRestResource.class);
                    return war;
                }
            });

    @Test
    public void testSubResourceFilter() {
        RestAssured.get("/sub-resource/Bob/Builder")
                .then()
                .header("single-filter", Matchers.equalTo("once"))
                .body(Matchers.equalTo("Bob Builder"))
                .statusCode(200);
    }

    @Path("/")
    public static class RestResource {

        @Inject
        MiddleRestResource restSubResource;

        @Path("sub-resource/{first}")
        public MiddleRestResource hello(String first) {
            return restSubResource;
        }
    }

    @ApplicationScoped
    @Path("/")
    public static class MiddleRestResource {

        @Inject
        RestSubResource restSubResource;

        @Path("{last}")
        public RestSubResource hello() {
            return restSubResource;
        }
    }

    @ApplicationScoped
    public static class RestSubResource {

        @GET
        public Response hello(HttpHeaders headers, @RestPath String first, @RestPath String last) {
            return Response.ok(first + " " + last).header("single-filter", headers.getHeaderString("single-filter")).build();
        }
    }

    @Provider
    public static class SingleExecutionFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getProperty("been.here") != null) {
                throw new IllegalStateException("Filter should not have been called twice");
            }
            requestContext.setProperty("been.here", Boolean.TRUE);
            requestContext.getHeaders().putSingle("single-filter", "once");
        }
    }
}
