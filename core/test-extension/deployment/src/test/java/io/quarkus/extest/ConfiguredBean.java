package io.quarkus.extest;

import io.quarkus.extest.runtime.IConfigConsumer;
import io.quarkus.extest.runtime.TestAnnotation;
import io.quarkus.extest.runtime.config.AnotherPrefixConfig;
import io.quarkus.extest.runtime.config.FooRuntimeConfig;
import io.quarkus.extest.runtime.config.PrefixConfig;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;
import io.quarkus.extest.runtime.config.named.PrefixNamedConfig;

/**
 * A sample bean
 */
@TestAnnotation
public class ConfiguredBean implements IConfigConsumer {
    volatile TestRunTimeConfig runTimeConfig;
    volatile TestBuildAndRunTimeConfig buildTimeConfig;
    volatile FooRuntimeConfig fooRuntimeConfig;
    volatile PrefixConfig prefixConfig;
    volatile PrefixNamedConfig prefixNamedConfig;
    volatile AnotherPrefixConfig anotherPrefixConfig;

    /**
     * Called by runtime with the runtime config object
     * 
     * @param runTimeConfig
     */
    @Override
    public void loadConfig(TestBuildAndRunTimeConfig buildTimeConfig, TestRunTimeConfig runTimeConfig,
            FooRuntimeConfig fooRuntimeConfig, PrefixConfig prefixConfig, PrefixNamedConfig prefixNamedConfig,
            AnotherPrefixConfig anotherPrefixConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runTimeConfig = runTimeConfig;
        this.fooRuntimeConfig = fooRuntimeConfig;
        this.prefixConfig = prefixConfig;
        this.prefixNamedConfig = prefixNamedConfig;
        this.anotherPrefixConfig = anotherPrefixConfig;
    }

    public TestRunTimeConfig getRunTimeConfig() {
        return runTimeConfig;
    }

    public TestBuildAndRunTimeConfig getBuildTimeConfig() {
        return buildTimeConfig;
    }

    public FooRuntimeConfig getFooRuntimeConfig() {
        return fooRuntimeConfig;
    }

    public PrefixConfig getPrefixConfig() {
        return prefixConfig;
    }

    public PrefixNamedConfig getPrefixNamedConfig() {
        return prefixNamedConfig;
    }

    public AnotherPrefixConfig getAnotherPrefixConfig() {
        return anotherPrefixConfig;
    }
}
