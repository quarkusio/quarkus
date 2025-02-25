package io.quarkus.deployment.dev.testing;

import java.util.function.BiConsumer;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.banner.BannerConfig;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.steps.BannerProcessor;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.BannerRecorder;
import io.quarkus.runtime.BannerRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.SmallRyeConfig;

public class TestHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        QuarkusConsole.start();
        TestSupport.instance().get().start();

        //we don't actually start the app
        //so logging would not be enabled
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        BannerConfig banner = config.getConfigMapping(BannerConfig.class);
        LoggingSetupRecorder.handleFailedStart(
                new BannerProcessor()
                        .recordBanner(new BannerRecorder(new RuntimeValue<>(new BannerRuntimeConfig() {
                            @Override
                            public boolean enabled() {
                                return config.getOptionalValue("quarkus.banner.enabled", Boolean.class).orElse(true);
                            }
                        })), banner).getBannerSupplier());
        Logger.getLogger("io.quarkus.test").info("Quarkus continuous testing mode started");
    }
}
