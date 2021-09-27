package io.quarkus.deployment.console;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.deployment.dev.testing.TestConsoleHandler;
import io.quarkus.deployment.dev.testing.TestListenerBuildItem;
import io.quarkus.deployment.dev.testing.TestSetupBuildItem;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;

public class ConsoleProcessor {

    private static boolean consoleInstalled = false;

    /**
     * Installs the interactive console for continuous testing (and other usages)
     * <p>
     * This is only installed for dev and test mode, and runs in the build process rather than
     * a recorder to install this as early as possible
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    @Produce(TestSetupBuildItem.class)
    ConsoleInstalledBuildItem setupConsole(TestConfig config,
            BuildProducer<TestListenerBuildItem> testListenerBuildItemBuildProducer,
            LaunchModeBuildItem launchModeBuildItem, ConsoleConfig consoleConfig) {

        if (consoleInstalled) {
            return ConsoleInstalledBuildItem.INSTANCE;
        }
        consoleInstalled = true;
        if (config.console.orElse(consoleConfig.enabled)) {
            //this is a bit of a hack, but we can't just inject this normally
            //this is a runtime property value, but also a build time property value
            //as when running in dev mode they are both basically equivalent
            ConsoleRuntimeConfig consoleRuntimeConfig = new ConsoleRuntimeConfig();
            consoleRuntimeConfig.color = ConfigProvider.getConfig().getOptionalValue("quarkus.console.color", Boolean.class);
            io.quarkus.runtime.logging.ConsoleConfig loggingConsoleConfig = new io.quarkus.runtime.logging.ConsoleConfig();
            loggingConsoleConfig.color = ConfigProvider.getConfig().getOptionalValue("quarkus.log.console.color",
                    Boolean.class);
            ConsoleHelper.installConsole(config, consoleConfig, consoleRuntimeConfig, loggingConsoleConfig,
                    launchModeBuildItem.isTest());
            ConsoleStateManager.init(QuarkusConsole.INSTANCE, launchModeBuildItem.getDevModeType().get());
            //note that this bit needs to be refactored so it is no longer tied to continuous testing
            if (!TestSupport.instance().isPresent() || config.continuousTesting == TestConfig.Mode.DISABLED
                    || config.flatClassPath) {
                return ConsoleInstalledBuildItem.INSTANCE;
            }
            TestConsoleHandler consoleHandler = new TestConsoleHandler(launchModeBuildItem.getDevModeType().get());
            consoleHandler.install();
            testListenerBuildItemBuildProducer.produce(new TestListenerBuildItem(consoleHandler));
        }
        return ConsoleInstalledBuildItem.INSTANCE;
    }
}
