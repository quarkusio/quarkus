package io.quarkus.resteasy.reactive.jackson.deployment.test.serializers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.resteasy.reactive.jackson.common.RestClientObjectMapper;
import io.quarkus.resteasy.reactive.jackson.common.RestServerObjectMapper;
import io.quarkus.test.QuarkusUnitTest;

public class DistinctObjectMappersProducerTest {

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
    public void unqualifiedObjectMapper() {
        assertionList(unqualifiedObjectMapper).contains("unqualified").doesNotContain("client").doesNotContain("server");
    }

    @Test
    public void clientObjectMapper() {
        assertionList(clientObjectMapper).doesNotContain("unqualified").contains("client").doesNotContain("server");
    }

    @Test
    public void serverObjectMapper() {
        assertionList(serverObjectMapper).doesNotContain("unqualified").doesNotContain("client").contains("server");
    }

    @Singleton
    public static class Factory {
        private ObjectMapper createObjectMapper(String moduleName) {
            return new ObjectMapper().registerModule(new SimpleModule(moduleName));
        }

        @Produces
        ObjectMapper unqualifiedObjectMapper() {
            return createObjectMapper("unqualified");
        }

        @Produces
        @RestClientObjectMapper
        ObjectMapper clientObjectMapper() {
            return createObjectMapper("client");
        }

        @Produces
        @RestServerObjectMapper
        ObjectMapper serverObjectMapper() {
            return createObjectMapper("server");
        }
    }
}
