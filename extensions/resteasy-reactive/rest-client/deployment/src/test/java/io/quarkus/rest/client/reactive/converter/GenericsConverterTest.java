package io.quarkus.rest.client.reactive.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class GenericsConverterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    @Test
    void testSingle() {
        TestClient client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(TestClient.class);
        var result = client.wrapper(new WrapperClass<>(StatusEnum.ACTIVE));
        assertEquals("ACTIVE", result);
    }

    @Test
    void testList() {
        TestClient client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(TestClient.class);
        var result = client
                .wrapperList(List.of(new WrapperClass<>(StatusEnum.ACTIVE), new WrapperClass<>(StatusEnum.INACTIVE)));
        assertEquals("ACTIVE,INACTIVE", result);
    }

    public enum StatusEnum {

        ACTIVE,
        INACTIVE
    }

    public static class WrapperClass<E extends Enum<E>> {

        private final E value;

        public WrapperClass(final E value) {
            this.value = value;
        }

        public E getValue() {
            return value;
        }
    }

    public static class WrapperClassParamConverter implements ParamConverter<WrapperClass<?>> {

        @Override
        public WrapperClass<?> fromString(String value) {
            return new WrapperClass<>(Enum.valueOf(StatusEnum.class, value));
        }

        @Override
        public String toString(WrapperClass<?> wrapperClass) {
            return wrapperClass != null ? wrapperClass.getValue().toString() : null;
        }

    }

    @Provider
    public static class WrapperClassParamConverterProvider implements ParamConverterProvider {

        @Override
        @SuppressWarnings("unchecked")
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (WrapperClass.class.isAssignableFrom(rawType)) {
                return (ParamConverter<T>) new WrapperClassParamConverter();
            }

            return null;
        }
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/single")
        public String wrapper(@QueryParam("wrapper") final WrapperClass<StatusEnum> wrapper) {
            return wrapper.getValue().toString();
        }

        @GET
        @Path("/list")
        public String wrapperList(@QueryParam("wrapperList") final List<WrapperClass<StatusEnum>> wrapperList) {
            return wrapperList.stream().map(WrapperClass::getValue).map(Enum::name).collect(Collectors.joining(","));
        }

    }

    @Path("/test")
    public interface TestClient {

        @GET
        @Path("/single")
        String wrapper(@QueryParam("wrapper") final WrapperClass<StatusEnum> wrapper);

        @GET
        @Path("/list")
        String wrapperList(@QueryParam("wrapperList") final List<WrapperClass<StatusEnum>> wrapperList);

    }

}
