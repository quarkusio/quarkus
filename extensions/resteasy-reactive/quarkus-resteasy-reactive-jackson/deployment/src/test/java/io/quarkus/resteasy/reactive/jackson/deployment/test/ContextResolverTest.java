package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_INDEX;
import static io.restassured.RestAssured.with;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class ContextResolverTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(EnumsResource.class, Type.class, TypeContextResolver.class);
                }
            });

    @Test
    public void shouldUseCustomObjectMapper() {
        with().accept(ContentType.JSON).get("/enums/type/foo")
                .then().statusCode(200).body(equalTo("0"));
        with().accept(ContentType.JSON).get("/enums/type/bar")
                .then().statusCode(200).body(equalTo("1"));
    }

    @Test
    public void shouldUseDefaultObjectMapper() {
        with().accept(ContentType.JSON).get("/enums/color/red")
                .then().statusCode(200).body(equalTo("\"RED\""));
        with().accept(ContentType.JSON).get("/enums/color/black")
                .then().statusCode(200).body(equalTo("\"BLACK\""));
    }

    @Path("enums")
    public static class EnumsResource {

        @Path("type/foo")
        @GET
        public Type foo() {
            return Type.FOO;
        }

        @Path("type/bar")
        @GET
        public Type bar() {
            return Type.BAR;
        }

        @Path("color/red")
        @GET
        public Color red() {
            return Color.RED;
        }

        @Path("color/black")
        @GET
        public Color black() {
            return Color.BLACK;
        }
    }

    public enum Type {
        FOO,
        BAR
    }

    public enum Color {
        RED,
        BLACK
    }

    @Provider
    public static class TypeContextResolver implements ContextResolver<ObjectMapper> {
        @Override
        public ObjectMapper getContext(Class<?> type) {
            if (!type.isAssignableFrom(Type.class)) {
                return null;
            }
            ObjectMapper result = new ObjectMapper();
            result.enable(WRITE_ENUMS_USING_INDEX);
            return result;
        }
    }
}
