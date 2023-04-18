package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;

public class BlockingApplicationAndPreMatchingFilterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestApplication.class, PreMatchingFilter.class, RegularFilter.class, Resource.class);
                }
            });

    @Test
    public void test() {
        get("/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("pre-matching-filter-blocking=true/regular-filter-blocking=true/resource=true"));
    }

    @Blocking
    public static class TestApplication extends Application {

    }

    @Provider
    @PreMatching
    public static class PreMatchingFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext rc) throws IOException {
            rc.getHeaders().add("pre-matching-filter-blocking", String.valueOf(BlockingOperationControl.isBlockingAllowed()));
        }
    }

    @Provider
    public static class RegularFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext rc) throws IOException {
            rc.getHeaders().add("regular-filter-blocking", String.valueOf(BlockingOperationControl.isBlockingAllowed()));
        }
    }

    @Path("/test")
    public static class Resource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(HttpHeaders headers) {
            return "pre-matching-filter-blocking=" + headers.getHeaderString("pre-matching-filter-blocking") + "/"
                    + "regular-filter-blocking=" + headers.getHeaderString("regular-filter-blocking") + "/"
                    + "resource=" + BlockingOperationControl.isBlockingAllowed();
        }

    }
}
