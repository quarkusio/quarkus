package io.quarkus.it.jackson;

import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ObjectMapperModulesTest {

    private static final Object JDK8_MODULE_TYPE_ID = new Jdk8Module().getTypeId();
    private static final Object JAVA_TIME_MODULE_TYPE_ID = new JavaTimeModule().getTypeId();
    private static final Object PARAMETER_NAMES_MODULE_TYPE_ID = new ParameterNamesModule().getTypeId();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testExpectedModulesAreRegistered() {
        assertThat(objectMapper.getRegisteredModuleIds())
                .contains(JDK8_MODULE_TYPE_ID, JAVA_TIME_MODULE_TYPE_ID, PARAMETER_NAMES_MODULE_TYPE_ID);
    }
}
