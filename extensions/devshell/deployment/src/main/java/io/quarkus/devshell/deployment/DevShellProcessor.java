package io.quarkus.devshell.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.console.AeshConsole;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.console.DelegateConnection;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devshell.deployment.spi.ShellPageBuildItem;
import io.quarkus.devshell.deployment.tui.DelegateConnectionBackend;
import io.quarkus.devshell.deployment.tui.TerminalUI;
import io.quarkus.devshell.deployment.tui.screens.DependenciesScreen;
import io.quarkus.devshell.deployment.tui.screens.ExtensionsListScreen;
import io.quarkus.devshell.deployment.tui.screens.MainMenuScreen;
import io.quarkus.devui.deployment.ExtensionsBuildItem;
import io.quarkus.devui.deployment.extension.Extension;
import io.quarkus.devui.spi.buildtime.BuildTimeData;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

@BuildSteps(onlyIf = IsDevelopment.class)
class DevShellProcessor {

    private static final Logger log = Logger.getLogger(DevShellProcessor.class);
    private static final String FEATURE = "devshell";

    static volatile ConsoleStateManager.ConsoleContext shellContext;
    static volatile List<String> allowedHosts;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    void registerConsoleCommand(LaunchModeBuildItem launchModeBuildItem,
            DevShellBuildTimeConfig config,
            ExtensionsBuildItem extensionsBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<ShellPageBuildItem> shellPages,
            List<CardPageBuildItem> cardPages) {
        if (!config.enabled()) {
            return;
        }
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }
        if (!(QuarkusConsole.INSTANCE instanceof AeshConsole)) {
            return;
        }

        allowedHosts = config.allowedHosts().orElse(List.of());

        List<ExtensionsListScreen.ExtensionInfo> infos = new ArrayList<>();
        for (Extension ext : extensionsBuildItem.getActiveExtensions()) {
            infos.add(new ExtensionsListScreen.ExtensionInfo(
                    ext.getNamespace(), ext.getName(), ext.getDescription(),
                    ext.getKeywords(), true));
        }
        for (Extension ext : extensionsBuildItem.getInactiveExtensions()) {
            infos.add(new ExtensionsListScreen.ExtensionInfo(
                    ext.getNamespace(), ext.getName(), ext.getDescription(),
                    ext.getKeywords(), false));
        }
        DevShellContext.setExtensionInfos(infos);

        Map<String, DevShellContext.ShellPageInfo> pageMap = new HashMap<>();
        for (ShellPageBuildItem page : shellPages) {
            String id = page.getId(curateOutcomeBuildItem);
            pageMap.put(id, new DevShellContext.ShellPageInfo(
                    id, page.getTitle(),
                    page.getPageClassName(), page.getPageClass(),
                    page.getProviderClassName()));
        }
        DevShellContext.setShellPages(pageMap);

        Map<String, Map<String, Object>> btData = new HashMap<>();
        for (CardPageBuildItem card : cardPages) {
            if (card.hasBuildTimeData()) {
                String ns = card.getExtensionPathName(curateOutcomeBuildItem);
                Map<String, Object> nsData = btData.computeIfAbsent(ns, k -> new HashMap<>());
                for (Map.Entry<String, BuildTimeData> entry : card.getBuildTimeData().entrySet()) {
                    nsData.put(entry.getKey(), entry.getValue().getContent());
                }
            }
        }
        DevShellContext.setBuildTimeData(btData);

        List<DependenciesScreen.DependencyInfo> deps = new ArrayList<>();
        for (ResolvedDependency dep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
            deps.add(new DependenciesScreen.DependencyInfo(
                    dep.getGroupId(), dep.getArtifactId(), dep.getVersion(),
                    dep.getScope() != null ? dep.getScope() : ""));
        }
        deps.sort((a, b) -> (a.groupId() + ":" + a.artifactId()).compareTo(b.groupId() + ":" + b.artifactId()));
        DevShellContext.setDependencyInfos(deps);

        if (shellContext == null) {
            shellContext = ConsoleStateManager.INSTANCE.createContext("Dev Shell");
        }
        shellContext.reset(
                new ConsoleCommand('t', "Open Dev Shell TUI", "for the Dev Shell", 100, null,
                        DevShellProcessor::launchShell));
    }

    private static void launchShell() {
        if (!(QuarkusConsole.INSTANCE instanceof AeshConsole aeshConsole)) {
            return;
        }

        DelegateConnection delegate = aeshConsole.takeoverTerminal();
        DelegateConnectionBackend backend = new DelegateConnectionBackend(delegate);

        Thread tuiThread = new Thread(() -> {
            DevShellJsonRpcClient jsonRpcClient = null;
            try {
                jsonRpcClient = new DevShellJsonRpcClient("localhost", 8080, "/q/dev-ui/json-rpc-ws",
                        allowedHosts != null ? allowedHosts : List.of());
                jsonRpcClient.connect();

                TerminalUI tui = new TerminalUI(backend, jsonRpcClient);
                tui.start(new MainMenuScreen());
            } catch (Exception e) {
                log.error("Dev Shell error", e);
            } finally {
                if (jsonRpcClient != null) {
                    jsonRpcClient.close();
                }
                aeshConsole.releaseTerminal();
            }
        }, "Dev Shell TUI");
        tuiThread.setDaemon(true);
        tuiThread.start();
    }
}
