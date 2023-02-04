package io.quarkus.jackson.deployment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.JacksonMixin;
import io.quarkus.test.QuarkusUnitTest;

public class JacksonMixinsWithoutCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void test() throws JsonProcessingException {
        assertThat(objectMapper.writeValueAsString(new Fruit("test"))).isEqualTo("{\"nm\":\"test\"}");
        assertThat(objectMapper.writeValueAsString(new Fruit2("test"))).isEqualTo("{\"nm\":\"test\"}");
    }

    public static class Fruit {
        public String name;

        public Fruit(String name) {
            this.name = name;
        }
    }

    public static class Fruit2 {
        public String name;

        public Fruit2(String name) {
            this.name = name;
        }
    }

    @JacksonMixin({ Fruit.class, Fruit2.class })
    public abstract static class FruitMixin {
        @JsonProperty("nm")
        public String name;

    }
}
