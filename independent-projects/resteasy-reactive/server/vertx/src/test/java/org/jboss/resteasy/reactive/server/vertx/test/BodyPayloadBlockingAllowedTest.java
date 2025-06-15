package org.jboss.resteasy.reactive.server.vertx.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.util.Base64;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.mutiny.Uni;

public class BodyPayloadBlockingAllowedTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(TestResource.class,
                    TestInterceptor.class, TestRequestScopedBean.class));

    @Test
    void testSmallRequestForNonBlocking() {
        doTest(5_000, "non-blocking", false);
    }

    @Test
    void testLargeRequestForNonBlocking() {
        doTest(5_000_000, "non-blocking", false);
    }

    @Test
    void testSmallRequestForBlocking() {
        doTest(5_000, "blocking", true);
    }

    @Test
    void testLargeRequestForBlocking() {
        doTest(5_000_000, "blocking", true);
    }

    private static void doTest(int size, String path, boolean blockingAllowed) {
        given() //
                .body(String.format("{\"data\":\"%s\"}", getBase64String(size)))
                .header("Content-Type", MediaType.TEXT_PLAIN).when().post("/test/" + path).then().statusCode(200)
                .body(equalTo("" + blockingAllowed));
    }

    private static String getBase64String(int size) {
        return Base64.getEncoder().encodeToString(new byte[size]);
    }

    @Path("test")
    public static class TestResource {

        @Path("non-blocking")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<Boolean> nonBlocking(String request) {
            return Uni.createFrom().item(BlockingOperationSupport::isBlockingAllowed);
        }

        @Path("blocking")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public Boolean blocking(String request) {
            return BlockingOperationSupport.isBlockingAllowed();
        }

    }

    // the interceptor is used in order to test that a request scoped bean works properly no matter what thread the
    // payload is read from

    @Provider
    public static class TestInterceptor implements ReaderInterceptor {

        @Inject
        protected TestRequestScopedBean testRequestScopedBean;

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context)
                throws IOException, WebApplicationException {
            var entity = context.proceed();
            testRequestScopedBean.log((String) entity);
            return entity;
        }
    }

    @RequestScoped
    public static class TestRequestScopedBean {

        public void log(final String ignored) {

        }
    }

}
