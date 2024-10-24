package io.quarkus.deployment.dev.testing;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.banner.BannerConfig;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.steps.BannerProcessor;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.BannerRecorder;
import io.quarkus.runtime.BannerRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class TestHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        QuarkusConsole.start();
        TestSupport.instance().get().start();

        //we don't actually start the app
        //so logging would not be enabled
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        // There may be cases where a Config with the mappings is already available, but we can't be sure, so we wrap
        // the original Config and map the logging classes.
        SmallRyeConfig bannerConfig = new SmallRyeConfigBuilder()
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
                .withMapping(BannerConfig.class)
                .withMapping(BannerRuntimeConfig.class)
                .withSources(new ConfigSource() {
                    @Override
                    public Set<String> getPropertyNames() {
                        Set<String> properties = new HashSet<>();
                        config.getPropertyNames().forEach(properties::add);
                        return properties;
                    }

                    @Override
                    public String getValue(final String propertyName) {
                        return config.getRawValue(propertyName);
                    }

                    @Override
                    public String getName() {
                        return "Banner Config";
                    }
                }).build();

        BannerConfig banner = config.getConfigMapping(BannerConfig.class);
        BannerRuntimeConfig bannerRuntime = config.getConfigMapping(BannerRuntimeConfig.class);
        LoggingSetupRecorder.handleFailedStart(new BannerProcessor()
                .recordBanner(new BannerRecorder(new RuntimeValue<>(bannerRuntime)), banner).getBannerSupplier());
        Logger.getLogger("io.quarkus.test").info("Quarkus continuous testing mode started");
    }
}
