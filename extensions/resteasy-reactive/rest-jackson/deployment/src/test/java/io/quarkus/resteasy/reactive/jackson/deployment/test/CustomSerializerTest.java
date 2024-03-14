package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.assertj.core.api.Assertions;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.response.Response;

public class CustomSerializerTest {
    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.now();
    private static final Jackson2Mapper MAPPER = new Jackson2Mapper((type, charset) -> {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    });

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withEmptyApplication();

    @Test
    void shouldUseModulesInCustomSerializer() {
        final Response response = RestAssured.given().get("custom-serializer");
        Assertions.assertThat(response.statusCode()).isEqualTo(StatusCode.OK);

        final CustomData actual = response.as(CustomData.class, MAPPER);
        final CustomData expected = new CustomData("test-data", FIXED_TIME);
        Assertions.assertThat(actual)
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime.class)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Path("custom-serializer")
    @Produces(MediaType.APPLICATION_JSON)
    static class CustomJacksonEndpoint {

        @GET
        public CustomData getCustom() {
            return new CustomData("test-data", FIXED_TIME);
        }
    }

    static class CustomData {
        private final String name;
        private final OffsetDateTime time;

        @JsonCreator
        CustomData(@JsonProperty("name") final String name, @JsonProperty("time") final OffsetDateTime time) {
            this.name = name;
            this.time = time;
        }

        public String getName() {
            return this.name;
        }

        public OffsetDateTime getTime() {
            return this.time;
        }
    }

    static class CustomDataSerializer extends StdSerializer<CustomData> {
        CustomDataSerializer() {
            super(CustomData.class);
        }

        @Override
        public void serialize(final CustomData customData, final JsonGenerator jsonGenerator,
                final SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("name", customData.getName());
            if (customData.getTime() != null) {
                jsonGenerator.writeObjectField("time", customData.getTime());
            }
            jsonGenerator.writeEndObject();
        }
    }

    @Provider
    @Unremovable
    public static class CustomObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

        @Override
        public ObjectMapper getContext(final Class<?> type) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final SimpleModule simpleModule = new SimpleModule("custom-data");
            simpleModule.addSerializer(new CustomDataSerializer());
            objectMapper.registerModule(simpleModule);
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper;
        }
    }

}
