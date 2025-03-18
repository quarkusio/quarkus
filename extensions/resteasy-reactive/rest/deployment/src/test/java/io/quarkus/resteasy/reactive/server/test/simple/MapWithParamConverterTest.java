package io.quarkus.resteasy.reactive.server.test.simple;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MapWithParamConverterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloResource.class, MapParamConverter.class, MapParamConverterProvider.class));

    @Test
    public void noQueryParams() {
        RestAssured.get("/hello")
                .then().statusCode(200).body(Matchers.equalTo(""));
    }

    @Test
    public void jsonQueryParam() {
        RestAssured
                .with()
                .queryParam("param", "{\"a\":\"1\",\"b\":\"2\"}")
                .get("/hello")
                .then().statusCode(200).body(Matchers.equalTo("a:1-b:2"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        @Produces("text/plain")
        public String hello(@RestQuery("param") Map<String, Integer> names) {
            return Optional.ofNullable(names)
                    .orElse(Map.of())
                    .entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining("-"));
        }

    }

    @Provider
    static class MapParamConverterProvider implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType == Map.class)
                return new MapParamConverter<>(rawType, genericType);
            return null;
        }

    }

    static class MapParamConverter<T> implements ParamConverter<T> {

        Class<T> rawType;
        JavaType genericType;
        ObjectMapper objectMapper = new ObjectMapper();

        public MapParamConverter(Class<T> rawType, Type genericType) {
            this.genericType = genericType != null ? TypeFactory.defaultInstance().constructType(genericType) : null;
            this.rawType = rawType;
        }

        @Override
        public T fromString(String value) {
            if (rawType.isAssignableFrom(String.class)) {
                //noinspection unchecked
                return (T) value;
            }
            try {
                return genericType != null ? objectMapper.readValue(value, genericType)
                        : objectMapper.readValue(value, rawType);
            } catch (JsonProcessingException e) {
                throw (new RuntimeException(e));
            }
        }

        @Override
        public String toString(T value) {
            if (rawType.isAssignableFrom(String.class)) {
                return (String) value;
            }
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw (new RuntimeException(e));
            }
        }

    }
}
