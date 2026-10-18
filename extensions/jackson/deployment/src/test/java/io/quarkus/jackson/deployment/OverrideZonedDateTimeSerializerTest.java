package io.quarkus.jackson.deployment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.ZonedDateTime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class OverrideZonedDateTimeSerializerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void test() {
        assertThat(objectMapper.writeValueAsString(ZonedDateTime.now())).isEqualTo("\"dummy\"");
    }

    @Singleton
    static class TestCustomizer implements JsonMapperBuilderCustomizer {

        @Override
        public int priority() {
            return JsonMapperBuilderCustomizer.MINIMUM_PRIORITY;
        }

        @Override
        public void customize(JsonMapper.Builder builder) {
            SimpleModule module = new SimpleModule();
            module.addSerializer(ZonedDateTime.class, new ValueSerializer<>() {
                @Override
                public void serialize(ZonedDateTime value, JsonGenerator gen, SerializationContext ctxt)
                        throws JacksonException {
                    gen.writeString("dummy");
                }
            });
            builder.addModule(module);
        }
    }
}
