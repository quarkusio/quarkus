package io.quarkus.it.extension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.extest.runtime.config.TestRuntimeInfo;
import io.quarkus.registry.ValueRegistry;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RuntimeInfoParameterTest {
    ValueRegistry valueRegistry;
    TestRuntimeInfo runtimeInfo;

    @Test
    void runtimeInfoParameter(ValueRegistry valueRegistry, TestRuntimeInfo runtimeInfo) {
        assertNotNull(this.valueRegistry);
        assertNotNull(this.runtimeInfo);
        assertNotNull(valueRegistry);
        assertNotNull(runtimeInfo);
    }
}
