package io.quarkus.extest.runtime;

import io.quarkus.extest.runtime.config.AnotherPrefixConfig;
import io.quarkus.extest.runtime.config.FooRuntimeConfig;
import io.quarkus.extest.runtime.config.PrefixConfig;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;
import io.quarkus.extest.runtime.config.named.PrefixNamedConfig;

/**
 * Interface used to pass the runtime configuration to an application bean for validation
 */
public interface IConfigConsumer {
    void loadConfig(TestBuildAndRunTimeConfig buildTimeConfig, TestRunTimeConfig runTimeConfig,
            FooRuntimeConfig fooRuntimeConfig, PrefixConfig prefixConfig, PrefixNamedConfig prefixNamedConfig,
            AnotherPrefixConfig anotherPrefixConfig);
}
