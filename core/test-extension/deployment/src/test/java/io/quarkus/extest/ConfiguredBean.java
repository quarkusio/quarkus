package io.quarkus.extest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.extest.runtime.config.AnotherPrefixConfig;
import io.quarkus.extest.runtime.config.FooRuntimeConfig;
import io.quarkus.extest.runtime.config.PrefixConfig;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;
import io.quarkus.extest.runtime.config.named.PrefixNamedConfig;

@ApplicationScoped
public class ConfiguredBean {
    @Inject
    TestRunTimeConfig runTimeConfig;
    @Inject
    TestBuildAndRunTimeConfig buildTimeConfig;
    @Inject
    FooRuntimeConfig fooRuntimeConfig;
    @Inject
    PrefixConfig prefixConfig;
    @Inject
    PrefixNamedConfig prefixNamedConfig;
    @Inject
    AnotherPrefixConfig anotherPrefixConfig;

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
