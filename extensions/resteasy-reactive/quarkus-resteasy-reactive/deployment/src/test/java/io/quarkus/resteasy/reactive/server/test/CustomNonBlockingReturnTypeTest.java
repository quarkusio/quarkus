package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.NonBlockingReturnTypeBuildItem;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;

public class CustomNonBlockingReturnTypeTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, CustomType.class, CustomType2.class, HasMessageMessageBodyWriter.class);
                }
            }).addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(
                                    new NonBlockingReturnTypeBuildItem(DotName.createSimple(CustomType.class.getName())));
                        }
                    }).produces(NonBlockingReturnTypeBuildItem.class).build();
                }
            });

    @Test
    public void testNoAnnotation() {
        get("/test/noAnnotation")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("blocking allowed: false"));
    }

    @Test
    public void testOtherNoAnnotation() {
        get("/test/otherNoAnnotation")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("blocking allowed: true"));
    }

    @Test
    public void testWithBlockingAnnotation() {
        get("/test/withBlockingAnnotation")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("blocking allowed: true"));
    }

    @Path("test")
    public static class Resource {

        @Path("noAnnotation")
        @GET
        public CustomType noAnnotation() {
            return new CustomType("blocking allowed: " + BlockingOperationControl.isBlockingAllowed());
        }

        @Path("otherNoAnnotation")
        @GET
        public CustomType2 otherNoAnnotation() {
            return new CustomType2("blocking allowed: " + BlockingOperationControl.isBlockingAllowed());
        }

        @Path("withBlockingAnnotation")
        @GET
        @Blocking
        public CustomType withBlockingAnnotation() {
            return new CustomType("blocking allowed: " + BlockingOperationControl.isBlockingAllowed());
        }
    }

    public static class CustomType implements HasMessage {
        private final String message;

        public CustomType(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    public static class CustomType2 implements HasMessage {
        private final String message;

        public CustomType2(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    public interface HasMessage {

        String getMessage();
    }

    @Provider
    public static class HasMessageMessageBodyWriter<T extends HasMessage> implements ServerMessageBodyWriter<T> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
                MediaType mediaType) {
            return HasMessage.class.isAssignableFrom(type);
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return HasMessage.class.isAssignableFrom(type);
        }

        @Override
        public void writeResponse(T o, Type genericType, ServerRequestContext context)
                throws WebApplicationException {
            context.serverResponse().end(o.getMessage());
        }

        @Override
        public void writeTo(T o, Class<?> type, Type genericType, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            entityStream.write(o.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

}
