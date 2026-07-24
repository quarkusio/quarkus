package io.quarkus.resteasy.reactive.jackson.deployment.test;

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

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

public class CustomSerializerTest {
    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.now();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest().withEmptyApplication();

    @Test
    void shouldUseModulesInCustomSerializer() {
        final Response response = RestAssured.given().get("custom-serializer");
        Assertions.assertThat(response.statusCode()).isEqualTo(StatusCode.OK);

        final CustomData actual = MAPPER.readValue(response.asString(), CustomData.class);
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
                final SerializationContext serializationContext) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringProperty("name", customData.getName());
            if (customData.getTime() != null) {
                jsonGenerator.writePOJOProperty("time", customData.getTime());
            }
            jsonGenerator.writeEndObject();
        }
    }

    @Provider
    @Unremovable
    public static class CustomObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

        @Override
        public ObjectMapper getContext(final Class<?> type) {
            final SimpleModule simpleModule = new SimpleModule("custom-data");
            simpleModule.addSerializer(new CustomDataSerializer());
            return JsonMapper.builder()
                    .addModule(simpleModule)
                    .build();
        }
    }

}
