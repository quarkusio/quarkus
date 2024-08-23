package io.quarkus.vertx.http.deployment.console;

import static io.quarkus.devui.deployment.ide.IdeProcessor.openBrowser;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class ConsoleProcessor {

    static volatile ConsoleStateManager.ConsoleContext context;

    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    void setupConsole(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }
        if (context == null) {
            context = ConsoleStateManager.INSTANCE.createContext("HTTP");
        }
        Config c = ConfigProvider.getConfig();
        String host = c.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        String port = c.getOptionalValue("quarkus.http.port", String.class).orElse("8080");
        context.reset(
                new ConsoleCommand('w', "Open the application in a browser", null, () -> openBrowser(rp, np, "/", host, port)),
                new ConsoleCommand('d', "Open the Dev UI in a browser", null,
                        () -> openBrowser(rp, np, "/q/dev-ui", host, port)));
    }
}
