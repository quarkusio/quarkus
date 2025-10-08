package io.quarkus.jackson.deployment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class OverrideZonedDateTimeSerializerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void test() throws JacksonException {
        assertThat(new ArrayList<>(objectMapper.getRegisteredModuleIds())).asList()
                .contains("jackson-datatype-jsr310");
        assertThat(objectMapper.writeValueAsString(ZonedDateTime.now())).isEqualTo("\"dummy\"");
    }

    @Singleton
    static class TestCustomizer implements ObjectMapperCustomizer {

        @Override
        public int priority() {
            return ObjectMapperCustomizer.MINIMUM_PRIORITY;
        }

        @Override
        public void customize(ObjectMapper objectMapper) {
            SimpleModule module = new SimpleModule();
            module.addSerializer(ZonedDateTime.class, new ValueSerializer<ZonedDateTime>() {
                @Override
                public void serialize(ZonedDateTime t, JsonGenerator jg, SerializationContext sp) throws IOException {
                    jg.writeString("dummy");
                }

            });
            objectMapper.registerModule(module);
        }
    }
}
