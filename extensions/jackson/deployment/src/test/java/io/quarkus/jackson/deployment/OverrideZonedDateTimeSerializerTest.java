package io.quarkus.jackson.deployment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.jackson.JsonMapperCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class OverrideZonedDateTimeSerializerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    JsonMapper objectMapper;

    @Test
    public void test() throws JsonProcessingException {
        assertThat(new ArrayList<>(objectMapper.getRegisteredModuleIds())).asList()
                .contains("jackson-datatype-jsr310");
        assertThat(objectMapper.writeValueAsString(ZonedDateTime.now())).isEqualTo("\"dummy\"");
    }

    @Singleton
    static class TestCustomizer implements JsonMapperCustomizer {

        @Override
        public int priority() {
            return JsonMapperCustomizer.MINIMUM_PRIORITY;
        }

        @Override
        public void customize(JsonMapper.Builder builder) {
            SimpleModule module = new SimpleModule();
            module.addSerializer(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
                @Override
                public void serialize(ZonedDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException {
                    jg.writeString("dummy");
                }

            });
            builder.addModule(module);
        }
    }
}
