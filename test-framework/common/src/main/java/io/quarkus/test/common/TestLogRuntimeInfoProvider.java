package io.quarkus.test.common;

import io.quarkus.value.registry.RuntimeInfoProvider;
import io.quarkus.value.registry.ValueRegistry;

/**
 * Registers the {@link TestLog} with {@link ValueRegistry}.
 * <p>
 * In normal mode, the {@link TestLog} is also registered with a CDI Bean to support injection.
 */
public class TestLogRuntimeInfoProvider implements RuntimeInfoProvider {
    @Override
    public void register(ValueRegistry valueRegistry, RuntimeSource runtimeSource) {
        valueRegistry.registerInfo(TestLog.TEST_LOG, TestLog.INFO);
    }
}