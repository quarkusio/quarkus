package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.io.IOException;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SubResourceRequestFilterTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(RestResource.class, RestSubResource.class, SingleExecutionFilter.class);
                    return war;
                }
            });

    @Test
    public void testAbortingRequestFilter() {
        RestAssured.get("/sub-resource/hello")
                .then()
                .header("single-filter", Matchers.equalTo("once"))
                .statusCode(200);
    }

    @Path("/")
    public static class RestResource {

        @Inject
        RestSubResource restSubResource;

        @Path("sub-resource/hello")
        public RestSubResource hello() {
            return restSubResource;
        }
    }

    @ApplicationScoped
    public static class RestSubResource {

        @GET
        public Response hello(HttpHeaders headers) {
            return Response.ok().header("single-filter", headers.getHeaderString("single-filter")).build();
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
