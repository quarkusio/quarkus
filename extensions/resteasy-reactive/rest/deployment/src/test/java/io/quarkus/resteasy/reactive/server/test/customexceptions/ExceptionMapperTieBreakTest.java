package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.quarkus.resteasy.reactive.server.test.ExceptionUtil.removeStackTrace;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * A mapper declared by the application is registered before the mappers contributed by extensions, so it wins over an
 * extension mapper for the same exception type and priority.
 */
public class ExceptionMapperTieBreakTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, ApplicationMapper.class, ExtensionMapper.class, ExceptionUtil.class);
                }
            })
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder builder) {
                    builder.addBuildStep(context -> context.produce(new ExceptionMapperBuildItem(
                            ExtensionMapper.class.getName(), IllegalStateException.class.getName(), Priorities.USER, true)))
                            .produces(ExceptionMapperBuildItem.class)
                            .build();
                }
            });

    @Test
    public void testApplicationMapperWins() {
        RestAssured.get("/test")
                .then().statusCode(419);
    }

    @Path("test")
    public static class Resource {
        @GET
        public String fail() {
            throw removeStackTrace(new IllegalStateException("boom"));
        }
    }

    @Provider
    public static class ApplicationMapper implements ExceptionMapper<IllegalStateException> {

        @Override
        public Response toResponse(IllegalStateException exception) {
            return Response.status(419).build();
        }
    }

    public static class ExtensionMapper implements ExceptionMapper<IllegalStateException> {

        @Override
        public Response toResponse(IllegalStateException exception) {
            return Response.status(418).build();
        }
    }
}
