package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.function.Supplier;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RequestScopeExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(RequestScopeExceptionMapper.class, HelloResource.class);
                }
            });

    @Test
    public void helloTest() {
        RestAssured.patch("/hello")
                .then().statusCode(999).header("path", "/hello");
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }
    }

    @Provider
    @RequestScoped
    public static class RequestScopeExceptionMapper implements ExceptionMapper<NotAllowedException> {

        @Inject
        UriInfo uriInfo;

        @Override
        public Response toResponse(NotAllowedException exception) {
            return Response.status(999).header("path", uriInfo.getPath()).build();
        }
    }
}
