package io.quarkus.deployment.console;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.BrowserOpenerBuildItem;
import io.quarkus.deployment.dev.console.ConsoleHelper;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.deployment.dev.testing.TestConsoleHandler;
import io.quarkus.deployment.dev.testing.TestListenerBuildItem;
import io.quarkus.deployment.dev.testing.TestSetupBuildItem;
import io.quarkus.deployment.dev.testing.TestSupport;
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
    void setupConsole(TestConfig config, BuildProducer<TestListenerBuildItem> testListenerBuildItemBuildProducer,
            LaunchModeBuildItem launchModeBuildItem, Capabilities capabilities, ConsoleConfig consoleConfig,
            Optional<BrowserOpenerBuildItem> browserOpener) {
        //note that this bit needs to be refactored so it is no longer tied to continuous testing
        if (!TestSupport.instance().isPresent() || config.continuousTesting == TestConfig.Mode.DISABLED
                || config.flatClassPath) {
            return;
        }
        if (consoleInstalled) {
            return;
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
            ConsoleHelper.installConsole(config, consoleConfig, consoleRuntimeConfig, loggingConsoleConfig);
            TestConsoleHandler consoleHandler = new TestConsoleHandler(launchModeBuildItem.getDevModeType().get(),
                    browserOpener.map(BrowserOpenerBuildItem::getBrowserOpener).orElse(null),
                    capabilities.isPresent(Capability.VERTX_HTTP));
            consoleHandler.install();
            testListenerBuildItemBuildProducer.produce(new TestListenerBuildItem(consoleHandler));
        }
    }
}
