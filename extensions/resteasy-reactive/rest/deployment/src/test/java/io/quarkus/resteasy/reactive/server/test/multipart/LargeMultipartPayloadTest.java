package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class LargeMultipartPayloadTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addAsResource(new StringAsset("""
                                    quarkus.http.limits.max-body-size=30M
                                    """),
                                    "application.properties");
                }
            }).addBuildChainCustomizer(buildChainBuilder -> buildChainBuilder.addBuildStep(context -> context.produce(
                    new MethodScannerBuildItem(new MethodScanner() {
                        @Override
                        public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                                Map<String, Object> methodContext) {
                            return List.of(new AlwaysFailHandler());
                        }
                    }))).produces(MethodScannerBuildItem.class).build());

    @Test
    public void testConnectionClosedOnException() {
        given()
                .multiPart("file", twentyMegaBytes())
                .post("/test")
                .then()
                .statusCode(200)
                .body(Matchers.is("Expected failure!"));
    }

    private static String twentyMegaBytes() {
        return new String(new byte[20_000_000]);
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadFile(@RestForm("file") FileUpload file) {
            return "File " + file.fileName() + " uploaded!";
        }
    }

    public static class Mappers {

        @ServerExceptionMapper(RuntimeException.class)
        Uni<Response> handle() {
            return Uni.createFrom().item(Response.status(200).entity("Expected failure!").build());
        }

    }

    public static class AlwaysFailHandler implements ServerRestHandler, HandlerChainCustomizer {

        @Override
        public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
            requestContext.suspend();
            requestContext.resume(new RuntimeException("Expected exception!"), true);
        }

        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
            return List.of(new AlwaysFailHandler());
        }
    }
}
