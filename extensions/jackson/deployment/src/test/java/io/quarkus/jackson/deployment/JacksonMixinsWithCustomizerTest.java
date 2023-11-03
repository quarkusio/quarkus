package io.quarkus.jackson.deployment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.JacksonMixin;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class JacksonMixinsWithCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void test() throws JsonProcessingException {
        assertThat(objectMapper.writeValueAsString(new Fruit("test"))).isEqualTo("{\"manual\":\"test\"}");
        assertThat(objectMapper.writeValueAsString(new Message("hello"))).isEqualTo("{}");
    }

    @Singleton
    static class TestCustomizer implements ObjectMapperCustomizer {

        @Override
        public void customize(ObjectMapper objectMapper) {
            objectMapper.addMixIn(Fruit.class, ManualFruitMixin.class);
        }
    }

    public static class Fruit {
        public String name;

        public Fruit(String name) {
            this.name = name;
        }
    }

    @JacksonMixin(Fruit.class)
    public abstract static class AutoFruitMixin {
        @JsonProperty("auto")
        public String name;
    }

    // this mixin will override the AutoFruitMixin because it will be explicitly registered by the user with a customizer
    public abstract static class ManualFruitMixin {
        @JsonProperty("manual")
        public String name;
    }

    public static class Message {
        private final String description;

        public Message(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @JacksonMixin(Message.class)
    public interface MessageMixin {
        @JsonIgnore
        String getDescription();
    }
}
