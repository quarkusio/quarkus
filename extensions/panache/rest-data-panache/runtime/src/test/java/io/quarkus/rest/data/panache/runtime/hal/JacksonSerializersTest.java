package io.quarkus.rest.data.panache.runtime.hal;

import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class JacksonSerializersTest extends AbstractSerializersTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(HalEntityWrapper.class, new HalEntityWrapperJacksonSerializer(new BookHalLinksProvider()));
        module.addSerializer(HalCollectionWrapper.class,
                new HalCollectionWrapperJacksonSerializer(new BookHalLinksProvider()));
        objectMapper.registerModule(module);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean usePublishedBook() {
        return true;
    }
}
