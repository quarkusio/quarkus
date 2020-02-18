package io.quarkus.extest.runtime;

import io.quarkus.extest.runtime.config.FooRuntimeConfig;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;

/**
 * Interface used to pass the runtime configuration to an application bean for validation
 */
public interface IConfigConsumer {
    void loadConfig(TestBuildAndRunTimeConfig buildTimeConfig, TestRunTimeConfig runTimeConfig,
            FooRuntimeConfig fooRuntimeConfig);
}
