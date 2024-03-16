package io.quarkus.rest.client.reactive.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class FormWithConverterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceClient.class, Resource.class, Input.class, InputParamConverterProvider.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void test() {
        ResourceClient client = RestClientBuilder.newBuilder().baseUri(baseUri).register(InputParamConverterProvider.class)
                .build(ResourceClient.class);
        String result = client.hello(new Input("hey"));
        assertThat(result).isEqualTo("hey!");
    }

    public interface ResourceClient {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        String hello(@FormParam("message") Input input);
    }

    @Path("")
    public static class Resource {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@FormParam("message") String message) {
            return message + "!";
        }
    }

    public static class Input {

        public String value;

        public Input(String value) {
            this.value = value;
        }
    }

    public static class InputParamConverterProvider implements ParamConverterProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType == Input.class) {
                return (ParamConverter<T>) new ParamConverter<Input>() {
                    @Override
                    public Input fromString(String value) {
                        return new Input(value);
                    }

                    @Override
                    public String toString(Input value) {
                        return value.value;
                    }
                };
            }
            return null;
        }
    }
}
