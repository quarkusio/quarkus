package io.quarkus.deployment.dev.testing;

import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.banner.BannerConfig;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.steps.BannerProcessor;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.BannerRecorder;
import io.quarkus.runtime.BannerRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.runtime.logging.LoggingSetupRecorder;

public class TestHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        QuarkusConsole.start();
        TestSupport.instance().get().start();

        //we don't actually start the app
        //so logging would not be enabled
        BannerConfig bannerConfig = new BannerConfig();
        BannerRuntimeConfig bannerRuntimeConfig = new BannerRuntimeConfig();
        ConfigInstantiator.handleObject(bannerConfig);
        ConfigInstantiator.handleObject(bannerRuntimeConfig);
        LoggingSetupRecorder.handleFailedStart(new BannerProcessor()
                .recordBanner(new BannerRecorder(), bannerConfig, bannerRuntimeConfig).getBannerSupplier());
        Logger.getLogger("io.quarkus.test").info("Quarkus continuous testing mode started");

    }
}
