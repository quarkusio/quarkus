package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.PreExceptionMapperHandlerBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class PreExceptionMapperHandlersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, Mappers.class, DummyPreExceptionMapperHandler1.class,
                                    DummyPreExceptionMapperHandler2.class);
                }
            }).addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(
                                    new PreExceptionMapperHandlerBuildItem(new DummyPreExceptionMapperHandler1(), 1000));
                            context.produce(
                                    new PreExceptionMapperHandlerBuildItem(new DummyPreExceptionMapperHandler2(), 2000));
                        }
                    }).produces(PreExceptionMapperHandlerBuildItem.class).build();
                }
            });

    @Test
    public void test() {
        get("/test")
                .then()
                .statusCode(999)
                .header("foo", "bar1bar2");

        get("/test/uni")
                .then()
                .statusCode(999)
                .header("foo", "bar1bar2");
    }

    @Path("test")
    public static class Resource {

        @GET
        public String get() {
            throw new RuntimeException("dummy");
        }

        @Path("uni")
        @GET
        public Uni<String> uniGet() {
            return Uni.createFrom().item(() -> {
                throw new RuntimeException("dummy");
            });
        }
    }

    public static class Mappers {

        @ServerExceptionMapper(RuntimeException.class)
        Response handle(ResteasyReactiveContainerRequestContext requestContext) {
            return Response.status(999)
                    .header("foo", requestContext.getProperty("foo")).build();
        }

    }

    public static class DummyPreExceptionMapperHandler1 implements ServerRestHandler {

        @Override
        public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
            assertThat(requestContext.getThrowable()).isInstanceOf(RuntimeException.class);

            Object value = requestContext.getProperty("foo");
            if (value != null) {
                value = value.toString() + "bar1";
            } else {
                value = "bar1";
            }

            requestContext.setProperty("foo", value);
        }
    }

    public static class DummyPreExceptionMapperHandler2 implements ServerRestHandler {

        @Override
        public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
            assertThat(requestContext.getThrowable()).isInstanceOf(RuntimeException.class);

            Object value = requestContext.getProperty("foo");
            if (value != null) {
                value = value.toString() + "bar2";
            } else {
                value = "bar2";
            }

            requestContext.setProperty("foo", value);
        }
    }
}
