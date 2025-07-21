package io.quarkus.devui.deployment.menu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.config.ConfigDescriptionBean;
import io.quarkus.devui.runtime.config.ConfigDevUIRecorder;
import io.quarkus.devui.runtime.config.ConfigJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;

/**
 * This creates Extensions Page
 */
public class ConfigurationProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    InternalPageBuildItem createConfigurationPages(
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {

        InternalPageBuildItem configurationPages = new InternalPageBuildItem("Configuration", 20);

        configurationPages.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Form Editor")
                .icon("font-awesome-solid:sliders")
                .componentLink("qwc-configuration.js"));

        configurationPages.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Source Editor")
                .icon("font-awesome-solid:code")
                .componentLink("qwc-configuration-editor.js"));

        configurationPages.addBuildTimeData("allConfiguration", new ArrayList<ConfigDescription>());

        return configurationPages;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    void registerConfigs(List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            ConfigDevUIRecorder recorder) {

        List<ConfigDescription> configDescriptions = new ArrayList<>();
        for (ConfigDescriptionBuildItem item : configDescriptionBuildItems) {
            configDescriptions.add(
                    new ConfigDescription(item.getPropertyName(),
                            formatJavadoc(cleanUpAsciiDocIfNecessary(item.getDocs())),
                            item.getDefaultValue(),
                            isSetByDevServices(devServicesLauncherConfig, item.getPropertyName()),
                            item.getValueTypeName(),
                            item.getAllowedValues(),
                            item.getConfigPhase().name()));
        }

        Set<String> devServicesConfig = new HashSet<>();
        if (devServicesLauncherConfig.isPresent()) {
            devServicesConfig.addAll(devServicesLauncherConfig.get().getConfig().keySet());
        }

        recorder.registerConfigs(configDescriptions, devServicesConfig);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerBuildTimeActions(
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            ConfigDevUIRecorder recorder,
            CuratedApplicationShutdownBuildItem shutdown) {

        BuildTimeActionBuildItem configActions = new BuildTimeActionBuildItem(NAMESPACE);

        configActions.addAction("updateProperty", map -> {
            Map<String, String> values = Collections.singletonMap(map.get("name"), map.get("value"));
            updateConfig(values);
            return true;
        });
        configActions.addAction("updateProperties", map -> {
            String type = map.get("type");

            if (type.equalsIgnoreCase("properties")) {
                String content = map.get("content");

                Properties p = new Properties();
                try (StringReader sr = new StringReader(content)) {
                    p.load(sr); // Validate
                    setConfig(content);
                    return true;
                } catch (IOException ex) {
                    return false;
                }
            }
            return false;
        });
        buildTimeActionProducer.produce(configActions);

        syntheticBeanProducer.produce(
                SyntheticBeanBuildItem.configure(ConfigDescriptionBean.class).unremovable()
                        .supplier(recorder.configDescriptionBean())
                        .scope(Singleton.class)
                        .setRuntimeInit()
                        .done());

        CurrentConfig.EDITOR = ConfigurationProcessor::updateConfig;
        shutdown.addCloseTask(new Runnable() {
            @Override
            public void run() {
                CurrentConfig.EDITOR = null;
                CurrentConfig.CURRENT = Collections.emptyList();
            }
        }, true);
    }

    @BuildStep
    JsonRPCProvidersBuildItem registerJsonRpcService() {
        return new JsonRPCProvidersBuildItem(NAMESPACE, ConfigJsonRPCService.class);
    }

    private static final Pattern codePattern = Pattern.compile("(\\{@code )([^}]+)(\\})");
    private static final Pattern linkPattern = Pattern.compile("(\\{@link )([^}]+)(\\})");

    private static String formatJavadoc(String val) {
        if (val == null) {
            return val;
        }
        // Replace {@code} and {@link}
        val = codePattern.matcher(val).replaceAll("<code>$2</code>");
        val = linkPattern.matcher(val).replaceAll("<code>$2</code>");
        // Add br before @see and @deprecated
        val = val.lines().filter(s -> !s.startsWith("@see")).collect(Collectors.joining("\n"));
        val = val.replace("@deprecated", "<br><strong>Deprecated</strong>");
        return val;
    }

    private static String cleanUpAsciiDocIfNecessary(String docs) {
        if (docs == null || !docs.toLowerCase(Locale.ROOT).contains("@asciidoclet")) {
            return docs;
        }
        // TODO #26199 Ideally we'd use a proper AsciiDoc renderer, but for now we'll just clean it up a bit.
        return docs.replace("@asciidoclet", "")
                // Avoid problems with links.
                .replace("<<", "&lt;&lt;")
                .replace(">>", "&gt;&gt;")
                // Try to render line breaks... kind of.
                .replace("\n\n", "<p>")
                .replace("\n", "<br>");
    }

    // TODO does this need updating to the config source? Or just deleting? Or consolidating?
    private static boolean isSetByDevServices(Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            String propertyName) {
        if (devServicesLauncherConfig.isPresent()) {
            return devServicesLauncherConfig.get().getConfig().containsKey(propertyName);
        }
        return false;
    }

    public static void updateConfig(Map<String, String> values) {
        if (values != null && !values.isEmpty()) {
            try {
                Path configPath = getConfigPath();
                List<String> lines = Files.readAllLines(configPath);
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    int nameLine = -1;
                    for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
                        String line = lines.get(i);
                        if (line.startsWith(name + "=")) {
                            nameLine = i;
                            break;
                        }
                    }
                    if (nameLine != -1) {
                        if (value.isEmpty()) {
                            lines.remove(nameLine);
                        } else {
                            lines.set(nameLine, name + "=" + value);
                        }
                    } else {
                        if (!value.isEmpty()) {
                            lines.add(name + "=" + value);
                        }
                    }
                }

                try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                    for (String i : lines) {
                        writer.write(i);
                        writer.newLine();
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    private static void setConfig(String value) {
        try {
            Path configPath = getConfigPath();
            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                if (value == null || value.isEmpty()) {
                    writer.newLine();
                } else {
                    writer.write(value);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Path getConfigPath() throws IOException {
        List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
        if (resourcesDir.isEmpty()) {
            throw new IllegalStateException("Unable to manage configurations - no resource directory found");
        }

        // In the current project only
        Path path = resourcesDir.get(0);
        Path configPath = path.resolve("application.properties");
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath.getParent());
            configPath = Files.createFile(path.resolve("application.properties"));
        }
        return configPath;
    }

    private static final String NAMESPACE = "devui-configuration";
}
