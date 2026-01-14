package io.quarkus.extest.runtime.config;

import io.quarkus.registry.RuntimeInfoProvider;
import io.quarkus.registry.ValueRegistry;

public class TestRuntimeInfoProvider implements RuntimeInfoProvider {
    @Override
    public void register(ValueRegistry valueRegistry, RuntimeSource source) {
        valueRegistry.registerInfo(TestRuntimeInfo.TEST_RUNTIME_INFO, TestRuntimeInfo.INFO);
    }
}
