package io.quarkus.resteasy.reactive.server.test.providers;

import static io.restassured.RestAssured.get;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ParamConverterTest {

    private static final String STATIC_UUID = "42f425f1-5923-41ca-a43c-713888762c68";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UUIDResource.class, UUIDParamConverterProvider.class));

    @Test
    public void single() {
        get("/uuid/single?id=whatever")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(STATIC_UUID));
    }

    @Test
    public void set() {
        get("/uuid/set?id=whatever&id=whatever2")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(STATIC_UUID));
    }

    @Test
    public void list() {
        get("/uuid/list?id=whatever&id=whatever2")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(STATIC_UUID + "," + STATIC_UUID));
    }

    @Path("uuid")
    public static class UUIDResource {

        @Path("single")
        @GET
        public String single(@QueryParam("id") UUID uuid) {
            return uuid.toString();
        }

        @Path("set")
        @GET
        public String set(@QueryParam("id") Set<UUID> uuids) {
            return join(uuids.stream());
        }

        @Path("list")
        @GET
        public String list(@QueryParam("id") List<? extends UUID> uuids) {
            return join(uuids.stream());
        }

        private static String join(Stream<? extends UUID> uuids) {
            return uuids.map(UUID::toString).collect(Collectors.joining(","));
        }
    }

    @Provider
    public static class UUIDParamConverterProvider implements ParamConverterProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType.equals(UUID.class)) {
                return (ParamConverter<T>) new UUIDParamConverter();
            }

            return null;
        }

        public static class UUIDParamConverter implements ParamConverter<UUID> {
            @Override
            public UUID fromString(String value) {
                return UUID.fromString(STATIC_UUID);
            }

            @Override
            public String toString(UUID value) {
                return value.toString();
            }
        }

    }
}
