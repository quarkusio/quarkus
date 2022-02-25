package io.quarkus.resteasy.reactive.jackson.deployment.test.serializers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.resteasy.reactive.jackson.common.RestClientObjectMapper;
import io.quarkus.resteasy.reactive.jackson.common.RestServerObjectMapper;
import io.quarkus.test.QuarkusUnitTest;

public class DistinctObjectMappersCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    @RestClientObjectMapper
    ObjectMapper clientObjectMapper;

    @Inject
    @RestServerObjectMapper
    ObjectMapper serverObjectMapper;

    @Inject
    ObjectMapper unqualifiedObjectMapper;

    private static AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertionList(ObjectMapper mapper) {
        return assertThat((List<?>) new ArrayList<>(mapper.getRegisteredModuleIds())).asList();
    }

    @Test
    public void unqualifiedObjectMapperCustomized() {
        assertionList(unqualifiedObjectMapper).contains("unqualified").doesNotContain("client").doesNotContain("server");
    }

    @Test
    public void clientObjectMapperCustomized() {
        assertionList(clientObjectMapper).contains("unqualified").contains("client").doesNotContain("server");
    }

    @Test
    public void serverObjectMapperCustomized() {
        assertionList(serverObjectMapper).contains("unqualified").doesNotContain("client").contains("server");
    }

    @Singleton
    static class DefaultCustomizer implements ObjectMapperCustomizer {
        @Override
        public void customize(ObjectMapper objectMapper) {
            objectMapper.registerModule(new SimpleModule("unqualified"));
        }
    }

    @Singleton
    @RestClientObjectMapper
    static class ClientCustomizer implements ObjectMapperCustomizer {
        @Override
        public void customize(ObjectMapper objectMapper) {
            objectMapper.registerModule(new SimpleModule("client"));
        }
    }

    @Singleton
    @RestServerObjectMapper
    static class ServerCustomizer implements ObjectMapperCustomizer {
        @Override
        public void customize(ObjectMapper objectMapper) {
            objectMapper.registerModule(new SimpleModule("server"));
        }
    }

    @Singleton
    public static class Factory {

        private ObjectMapper createMapper(Instance<ObjectMapperCustomizer> unqualifiedCustomizers,
                Instance<ObjectMapperCustomizer> qualifiedCustomizers) {
            ObjectMapper objectMapper = new ObjectMapper();
            // apply customizers in priority order
            Stream.concat(unqualifiedCustomizers.stream(), qualifiedCustomizers.stream())
                    .sorted()
                    .forEach(c -> c.customize(objectMapper));
            return objectMapper;
        }

        @Produces
        @RestClientObjectMapper
        ObjectMapper clientObjectMapper(Instance<ObjectMapperCustomizer> unqualifiedCustomizers,
                @RestClientObjectMapper Instance<ObjectMapperCustomizer> clientCustom) {
            return createMapper(unqualifiedCustomizers, clientCustom);
        }

        @Produces
        @RestServerObjectMapper
        ObjectMapper serverObjectMapper(Instance<ObjectMapperCustomizer> unqualifiedCustomizers,
                @RestServerObjectMapper Instance<ObjectMapperCustomizer> serverCustom) {
            return createMapper(unqualifiedCustomizers, serverCustom);
        }
    }
}
