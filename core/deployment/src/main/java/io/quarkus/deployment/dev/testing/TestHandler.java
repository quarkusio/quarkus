package io.quarkus.deployment.dev.testing;

import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.banner.BannerConfig;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.dev.testing.TestConfig.Mode;
import io.quarkus.deployment.jvm.JvmModulesReconfigurer;
import io.quarkus.deployment.jvm.ResolvedJVMRequirements;
import io.quarkus.deployment.steps.BannerProcessor;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.BannerRecorder;
import io.quarkus.runtime.BannerRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigInstanceBuilder;

public class TestHandler implements BiConsumer<Object, BuildResult> {

    private static final JvmModulesReconfigurer jvmModulesReconfigurer = JvmModulesReconfigurer.getInstance();

    @Override
    public void accept(Object o, BuildResult buildResult) {
        // Apply JVM module configuration for test mode (add-opens, etc.)
        ResolvedJVMRequirements jvmRequirements = buildResult.consume(ResolvedJVMRequirements.class);
        jvmRequirements.applyJavaModuleConfigurationToRuntime(jvmModulesReconfigurer,
                Thread.currentThread().getContextClassLoader());

        QuarkusConsole.start();
        TestSupport.instance().get().start();

        //we don't actually start the app
        //so logging would not be enabled
        Config config = Config.get();
        BannerConfig banner = config.getConfigMapping(BannerConfig.class);
        LoggingSetupRecorder.handleFailedStart(
                new BannerProcessor()
                        .recordBanner(new BannerRecorder(
                                new RuntimeValue<>(ConfigInstanceBuilder.forInterface(BannerRuntimeConfig.class)
                                        .with(BannerRuntimeConfig::enabled,
                                                config.getOptionalValue("quarkus.banner.enabled", Boolean.class).orElse(true))
                                        .build())),
                                banner)
                        .getBannerSupplier());
        if (!config.getOptionalValue("quarkus.test.continuous-testing", Mode.class).orElse(Mode.PAUSED)
                .equals(Mode.DISABLED)) {
            Logger.getLogger("io.quarkus.test").info("Quarkus continuous testing mode started");
        }
    }
}
