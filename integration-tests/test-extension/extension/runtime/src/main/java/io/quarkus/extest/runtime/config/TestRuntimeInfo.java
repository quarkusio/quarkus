package io.quarkus.extest.runtime.config;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeInfo;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

public interface TestRuntimeInfo {
    RuntimeKey<TestRuntimeInfo> TEST_RUNTIME_INFO = RuntimeKey.key(TestRuntimeInfo.class);

    RuntimeInfo<TestRuntimeInfo> INFO = new RuntimeInfo<TestRuntimeInfo>() {
        @Override
        public TestRuntimeInfo get(ValueRegistry valueRegistry) {
            return new TestRuntimeInfo() {

            };
        }
    };
}
