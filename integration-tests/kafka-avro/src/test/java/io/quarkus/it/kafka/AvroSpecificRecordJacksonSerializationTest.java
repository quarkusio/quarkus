package io.quarkus.it.kafka;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.it.kafka.avro.Pet;
import io.quarkus.test.junit.QuarkusTest;

/**
 * This test verifies that the Avro classes can be serialized using Jackson.
 * By default, they can't as the Avro class contains non-serializable fields.
 *
 * There is a custom serializer registered explicitly that allow the serialization.
 */
@QuarkusTest
public class AvroSpecificRecordJacksonSerializationTest {

    @Inject
    ObjectMapper mapper;

    @Test
    void testSerialization() throws JsonProcessingException {
        Assertions.assertTrue(mapper.getRegisteredModuleIds().contains("AvroSpecificRecordModule"));

        Pet pet = new Pet("roxanne", "gray");
        String s = mapper.writer().writeValueAsString(pet);
        // The serializer preserves the order of the field.
        Assertions.assertEquals(s, "{\"name\":\"roxanne\",\"color\":\"gray\"}");
    }

}
