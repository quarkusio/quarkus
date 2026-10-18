package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.quarkus.resteasy.reactive.server.test.ExceptionUtil.removeStackTrace;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

class SubResourceExceptionMapperTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RootResource.class, SubResource.class,
                    OtherSubResource.class, OtherResource.class, ExceptionUtil.class));

    @Test
    void subResourceMapperHandlesOwnException() {
        RestAssured.get("/root/sub/throw").then().statusCode(416);
    }

    @Test
    void subResourceMapperDoesNotAffectOtherResources() {
        RestAssured.get("/other/throw").then().statusCode(500);
    }

    @Test
    void subResourceMapperDoesNotAffectOtherSubResources() {
        RestAssured.get("/root/other-sub/throw").then().statusCode(500);
    }

    @Path("root")
    public static class RootResource {

        @Path("sub")
        public SubResource sub() {
            return new SubResource();
        }

        @Path("other-sub")
        public OtherSubResource otherSub() {
            return new OtherSubResource();
        }
    }

    public static class SubResource {

        @ServerExceptionMapper
        public Response handleException(IllegalArgumentException e) {
            return Response.status(416).build();
        }

        @GET
        @Path("throw")
        @Produces("text/plain")
        public String throwsException() {
            throw removeStackTrace(new IllegalArgumentException("from sub-resource"));
        }
    }

    public static class OtherSubResource {

        @GET
        @Path("throw")
        @Produces("text/plain")
        public String throwsException() {
            throw removeStackTrace(new IllegalArgumentException("from other sub-resource"));
        }
    }

    @Path("other")
    public static class OtherResource {

        @GET
        @Path("throw")
        @Produces("text/plain")
        public String throwsException() {
            throw removeStackTrace(new IllegalArgumentException("from other resource"));
        }
    }
}
