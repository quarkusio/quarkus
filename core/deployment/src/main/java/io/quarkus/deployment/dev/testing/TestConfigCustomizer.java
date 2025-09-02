package io.quarkus.deployment.dev.testing;

import java.util.function.Function;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class TestConfigCustomizer implements SmallRyeConfigBuilderCustomizer {
    private final LaunchMode launchMode;

    public TestConfigCustomizer(final LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("quarkus.profile", launchMode.getDefaultProfile());
        builder.withMapping(TestConfig.class);
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new FallbackConfigSourceInterceptor(new Function<String, String>() {
                    @Override
                    public String apply(final String name) {
                        if (name.equals("quarkus.test.integration-test-profile")) {
                            return "quarkus.profile";
                        }
                        return name;
                    }
                });
            }
        });
    }

    @Override
    public int priority() {
        return SmallRyeConfigBuilderCustomizer.super.priority();
    }
}
