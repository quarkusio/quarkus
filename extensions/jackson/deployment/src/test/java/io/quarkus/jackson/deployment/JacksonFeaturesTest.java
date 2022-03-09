package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonFeaturesTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-serialization-properties.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void objectMapperConfigCorrect() {
        assertThat(this.objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
        assertThat(this.objectMapper.isEnabled(SerializationFeature.WRAP_ROOT_VALUE)).isTrue();
        assertThat(this.objectMapper.isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE)).isTrue();
        assertThat(this.objectMapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)).isTrue();
        assertThat(this.objectMapper.isEnabled(JsonParser.Feature.ALLOW_COMMENTS)).isTrue();
        assertThat(this.objectMapper.isEnabled(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION)).isTrue();
    }

    @Test
    public void objectToJsonDoesntContainEmptyValue() throws JsonProcessingException {
        assertThat(this.objectMapper.writeValueAsString(new Pojo("", 1, "value // This is a comment")))
                .isEqualTo("{\"Pojo\":{\"anotherField\":\"value // This is a comment\",\"order\":1}}");
    }

    @Test
    public void objectToJsonPropertyOrderingCorrect() throws JsonProcessingException {
        assertThat(this.objectMapper.writeValueAsString(new Pojo("Darth Vader", 1, "value // This is a comment")))
                .isEqualTo("{\"Pojo\":{\"anotherField\":\"value // This is a comment\",\"name\":\"Darth Vader\",\"order\":1}}");
    }

    @Test
    public void jsonToObjectContainsEmptyValue() throws JsonProcessingException {
        assertThat(this.objectMapper.readValue("{\"Pojo\":{\"anotherField\":\"value // This is a comment\",\"order\":1}}",
                Pojo.class))
                        .isNotNull()
                        .extracting(Pojo::getName, Pojo::getOrder, Pojo::getAnotherField)
                        .containsExactly(null, 1, "value // This is a comment");
    }

    @Test
    public void jsonToObjectFailsOnUnknownProperty() {
        assertThatExceptionOfType(UnrecognizedPropertyException.class)
                .isThrownBy(() -> this.objectMapper.readValue("{\"Pojo\":{\"unknownProperty\":1}}", Pojo.class))
                .extracting(UnrecognizedPropertyException::getPropertyName)
                .isEqualTo("unknownProperty");
    }

    public static class Pojo {
        private String name;
        private int order;
        private String anotherField;

        public Pojo() {
        }

        public Pojo(String name, int order, String anotherField) {
            this.name = name;
            this.order = order;
            this.anotherField = anotherField;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getAnotherField() {
            return anotherField;
        }

        public void setAnotherField(String anotherField) {
            this.anotherField = anotherField;
        }
    }
}
