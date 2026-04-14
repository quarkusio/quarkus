package io.quarkus.devshell.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devshell.runtime.BuildTimeDataReader;
import io.quarkus.devshell.runtime.DevShellRecorder;
import io.quarkus.devshell.runtime.DevShellRouter;
import io.quarkus.devshell.spi.ShellPageBuildItem;
import io.quarkus.devui.deployment.ExtensionsBuildItem;

/**
 * Build processor for Quarkus Dev Shell.
 * Registers the 't' keyboard shortcut to launch the terminal UI.
 */
public class DevShellProcessor {

    private static final Logger LOG = Logger.getLogger(DevShellProcessor.class);

    private static final String FEATURE = "devshell";

    // Static to survive hot reloads — ConsoleContext must persist across augmentation cycles
    static volatile ConsoleStateManager.ConsoleContext shellContext;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Register DevShellRouter and BuildTimeDataReader as beans only in dev mode.
     * This avoids resolution errors in non-dev builds where JsonRpcRouter is not available.
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void registerDevShellBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexProducer,
            List<ShellPageBuildItem> shellPages) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(DevShellRouter.class)
                .addBeanClass(BuildTimeDataReader.class)
                .setDefaultScope(BuiltinScope.APPLICATION.getName())
                .setUnremovable()
                .build());

        // Register core TUI screen classes as CDI beans so they can use @Inject.
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        "io.quarkus.devshell.runtime.tui.screens.ContinuousTestingScreen",
                        "io.quarkus.devshell.runtime.tui.screens.ConfigurationScreen",
                        "io.quarkus.devshell.runtime.tui.screens.EndpointsScreen",
                        "io.quarkus.devshell.runtime.tui.screens.BuildInfoScreen")
                .setDefaultScope(BuiltinScope.DEPENDENT.getName())
                .setUnremovable()
                .build());

        // Register extension shell page classes as CDI beans — same pattern as
        // DevUIProcessor.additionalBean() for JsonRPC providers.
        // Log the runtime CP flags from the application model for diagnosis.
        for (ShellPageBuildItem page : shellPages) {
            if (page.hasCustomPage() && page.getPageClass() != null) {
                Class<?> c = page.getPageClass();
                additionalIndexProducer.produce(new AdditionalIndexedClassesBuildItem(c.getName()));
                additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                        .addBeanClass(c)
                        .setDefaultScope(BuiltinScope.DEPENDENT.getName())
                        .setUnremovable()
                        .build());
            }
        }
    }

    /**
     * Initialize the DevShellRouter and pass extension data at runtime.
     * This runs in the runtime classloader where Arc is available.
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeRouter(DevShellRecorder recorder, BeanContainerBuildItem beanContainer,
            LaunchModeBuildItem launchModeBuildItem,
            Optional<ExtensionsBuildItem> extensionsBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<ShellPageBuildItem> shellPages) {
        // Only initialize in local dev mode
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }

        // Initialize the router in the runtime classloader
        recorder.initializeRouter(beanContainer.getValue());

        // Pass extension data to runtime as primitive arrays (for cross-classloader compatibility)
        List<String[]> extensionData = new ArrayList<>();
        if (extensionsBuildItem.isPresent()) {
            ExtensionsBuildItem extensions = extensionsBuildItem.get();

            // Add active extensions
            extensions.getActiveExtensions().forEach(ext -> {
                extensionData.add(new String[] {
                        ext.getNamespace(),
                        ext.getName(),
                        ext.getDescription() != null ? ext.getDescription() : "",
                        ext.getStatus() != null ? ext.getStatus() : "",
                        "true"
                });
            });

            // Add inactive extensions
            extensions.getInactiveExtensions().forEach(ext -> {
                extensionData.add(new String[] {
                        ext.getNamespace(),
                        ext.getName(),
                        ext.getDescription() != null ? ext.getDescription() : "",
                        ext.getStatus() != null ? ext.getStatus() : "",
                        "false"
                });
            });
        }

        recorder.setExtensions(extensionData);

        // Pass shell page provider information to runtime as primitive arrays
        LOG.debugf("Processing %d ShellPageBuildItems", shellPages.size());
        Map<String, String[]> pageDataMap = new HashMap<>();
        for (ShellPageBuildItem page : shellPages) {
            String pageId = page.getId(curateOutcomeBuildItem);
            LOG.debugf("  ShellPageBuildItem: id=%s, title=%s, pageClassName=%s, providerClassName=%s",
                    pageId, page.getTitle(),
                    page.getPageClassName() != null ? page.getPageClassName() : "(none)",
                    page.getProviderClassName() != null ? page.getProviderClassName() : "(none)");
            pageDataMap.put(pageId, new String[] {
                    pageId,
                    page.getTitle(),
                    String.valueOf(page.getShortcutKey()),
                    page.getJsonRpcNamespace() != null ? page.getJsonRpcNamespace() : "",
                    page.getProviderClassName() != null ? page.getProviderClassName() : "",
                    page.getPageClassName() != null ? page.getPageClassName() : ""
            });
        }
        LOG.debugf("Passing %d shell pages to recorder", pageDataMap.size());
        recorder.setShellPages(pageDataMap);
    }

    @Produce(ServiceStartBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    void registerConsoleCommand(LaunchModeBuildItem launchModeBuildItem) {
        // Only register in local dev mode
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }

        if (shellContext == null) {
            shellContext = ConsoleStateManager.INSTANCE.createContext("Dev Shell");
        }

        // Register 't' key to launch the terminal UI
        // Uses the DevShellLauncher which sets up proper terminal handling
        // via DelegateConnection for raw keyboard input
        shellContext.reset(
                new ConsoleCommand('t', "Open Dev Shell (terminal UI)",
                        new ConsoleCommand.HelpState(() -> false),
                        DevShellLauncher::launch));
    }

}
