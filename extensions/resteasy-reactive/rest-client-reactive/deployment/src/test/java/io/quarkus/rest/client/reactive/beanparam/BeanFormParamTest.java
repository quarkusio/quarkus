package io.quarkus.rest.client.reactive.beanparam;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class BeanFormParamTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldPassFormParamsFromBeanParam() {
        assertThat(formTestClient().postFormParams(new BeanWithFormParams("value1", "value2", Param.SECOND)))
                .isEqualTo(
                        "received value1-value2-2");
    }

    private FormTestClient formTestClient() {
        return RestClientBuilder.newBuilder().baseUri(baseUri).register(ParamConverter.class).build(FormTestClient.class);
    }

    @Path("/form")
    public interface FormTestClient {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        String postFormParams(@BeanParam BeanWithFormParams beanParam);
    }

    public static class BeanWithFormParams {
        private final String param1;
        private final String param2;
        private final Param param3;

        public BeanWithFormParams(String param1, String param2, Param param3) {
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
        }

        @FormParam("param1")
        public String getParam1() {
            return param1;
        }

        @FormParam("param2")
        public String getParam2() {
            return param2;
        }

        @FormParam("param3")
        public Param getParam3() {
            return param3;
        }
    }

    @Path("/form")
    public static class FormTestResource {
        @POST
        public String post(@FormParam("param1") String param1, @FormParam("param2") String param2,
                @FormParam("param3") String param3) {
            return String.format("received %s-%s-%s", param1, param2, param3);
        }
    }

    enum Param {
        FIRST,
        SECOND
    }

    public static class ParamConverter implements ParamConverterProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> jakarta.ws.rs.ext.ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
                Annotation[] annotations) {
            if (rawType == BeanFormParamTest.Param.class) {
                return (jakarta.ws.rs.ext.ParamConverter<T>) new jakarta.ws.rs.ext.ParamConverter<BeanFormParamTest.Param>() {
                    @Override
                    public BeanFormParamTest.Param fromString(String value) {
                        return null;
                    }

                    @Override
                    public String toString(BeanFormParamTest.Param value) {
                        if (value == null) {
                            return null;
                        }
                        switch (value) {
                            case FIRST:
                                return "1";
                            case SECOND:
                                return "2";
                            default:
                                return "unexpected";
                        }
                    }
                };
            }
            return null;
        }
    }
}
