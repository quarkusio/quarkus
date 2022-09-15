package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultipleProducesTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Entity.class, FirstResource.class, SecondResource.class, ExcelMessageBodyWriter.class,
                                    TextPlainMessageBodyWriter.class, ApplicationJsonMessageBodyWriter.class);
                }
            });

    @Test
    public void firstResourceShouldReturnJson() {
        doTest("first", "application/json", "application/json");
    }

    @Test
    public void firstResourceShouldReturnMsExcel() {
        doTest("first", "application/vnd.ms-excel", "foo");
    }

    @Test
    public void firstResourceShouldReturnTextPlain() {
        doTest("first", "text/plain", "text/plain");
    }

    @Test
    public void secondResourceShouldReturnJson() {
        doTest("second", "application/json", "application/json");
    }

    @Test
    public void secondResourceShouldReturnMsExcel() {
        doTest("second", "application/vnd.ms-excel", "bar");
    }

    @Test
    public void secondResourceShouldReturnTextPlain() {
        doTest("second", "text/plain", "text/plain");
    }

    private void doTest(String path, String acceptType, String expectBody) {
        given()
                .accept(acceptType)
                .when().get(path)
                .then()
                .statusCode(200)
                .contentType(acceptType)
                .body(is(expectBody));
    }

    public static class Entity {

        public final String data;

        public Entity(String data) {
            this.data = data;
        }
    }

    @Path("first")
    public static class FirstResource {

        @GET
        @Produces({ "application/vnd.ms-excel", MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
        public Response get() {
            return Response.ok().entity(new Entity("foo")).build();
        }
    }

    @Path("second")
    public static class SecondResource {

        @GET
        @Produces({ MediaType.APPLICATION_JSON, "application/vnd.ms-excel", MediaType.TEXT_PLAIN })
        public Response get() {
            return Response.ok().entity(new Entity("bar")).build();
        }
    }

    @Provider
    public static class ExcelMessageBodyWriter implements MessageBodyWriter<Entity> {

        @Override
        public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
            return clazz.equals(Entity.class) &&
                    mediaType.getType().equals("application") &&
                    mediaType.getSubtype().equals("vnd.ms-excel");
        }

        @Override
        public void writeTo(Entity entity, Class<?> aClass, Type type, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap,
                OutputStream outputStream) throws IOException, WebApplicationException {

            outputStream.write(entity.data.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Provider
    public static class TextPlainMessageBodyWriter implements MessageBodyWriter<Entity> {

        @Override
        public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
            return clazz.equals(Entity.class) && mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE);
        }

        @Override
        public void writeTo(Entity myResponseEntity, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream)
                throws IOException, WebApplicationException {
            outputStream.write("text/plain".getBytes());
        }
    }

    @Provider
    public static class ApplicationJsonMessageBodyWriter implements MessageBodyWriter<Entity> {

        @Override
        public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
            return clazz.equals(Entity.class) && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
        }

        @Override
        public void writeTo(Entity myResponseEntity, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream)
                throws IOException, WebApplicationException {
            outputStream.write("application/json".getBytes());
        }
    }
}
