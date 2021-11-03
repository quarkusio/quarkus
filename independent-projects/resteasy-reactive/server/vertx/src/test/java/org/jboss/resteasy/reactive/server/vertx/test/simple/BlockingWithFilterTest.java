package org.jboss.resteasy.reactive.server.vertx.test.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import java.util.function.Supplier;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Disabled("filters")
public class BlockingWithFilterTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestFilter.class, TestResource.class);
                }
            });

    @Test
    public void requestFilterTest() {
        String response = RestAssured.get("/test/request")
                .then().statusCode(200).contentType("text/plain").extract().body().asString();
        String[] parts = response.split("/");
        assertEquals(2, parts.length);
        assertEquals(parts[0], parts[1]);
        assertFalse(parts[0].contains("eventloop"));
        assertTrue(parts[0].contains("executor"));
    }

    public static class TestFilter {

        @ServerRequestFilter
        public void filter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("filter-thread", Thread.currentThread().getName());
        }

    }

    @Path("/test")
    public static class TestResource {

        @Blocking
        @Path("/request")
        @GET
        public String get(@Context HttpHeaders headers) {
            return headers.getHeaderString("filter-thread") + "/" + Thread.currentThread().getName();
        }
    }
}
