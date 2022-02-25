package io.quarkus.resteasy.reactive.jackson.deployment.test.serializers;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.resteasy.reactive.jackson.common.RestClientObjectMapper;
import io.quarkus.resteasy.reactive.jackson.common.RestServerObjectMapper;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultObjectMappersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    @RestClientObjectMapper
    Instance<ObjectMapper> clientObjectMapper;

    @Inject
    @RestServerObjectMapper
    Instance<ObjectMapper> serverObjectMapper;

    @Test
    public void noClientObjectMapper() {
        Assertions.assertTrue(clientObjectMapper.isUnsatisfied());
    }

    @Test
    public void noServerObjectMapper() {
        Assertions.assertTrue(serverObjectMapper.isUnsatisfied());
    }
}
