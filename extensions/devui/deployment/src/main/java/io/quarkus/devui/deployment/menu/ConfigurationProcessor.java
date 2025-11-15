package io.quarkus.devui.deployment.menu;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;

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

        configActions.actionBuilder()
                .methodName("updateProperty")
                .description("Update a configuration in the Quarkus application")
                .parameter("name", "The name of the configuration to update")
                .parameter("value", "The new value for the configuration to update")
                .parameter("profile", "the profile of the configuration to update")
                .parameter("target", "The target configuration file to update")
                .function(map -> {
                    String name = map.get("name");
                    String value = map.get("value");
                    String profile = map.getOrDefault("profile", "");
                    String target = map.getOrDefault("target", "application.properties");
                    updateConfig(name, value, profile, target);
                    return true;
                })
                .enableMcpFuctionByDefault()
                .build();

        configActions.actionBuilder()
                .methodName("updateProperties")
                .description("Update multiple configurations in the Quarkus application")
                .parameter("type",
                        "The type should always be 'properties' as the content should be the contents of serialized properties object")
                .parameter("content",
                        "The string value of serialized properties, with the keys being the name of the configuration and the value the new value for that configuration")
                .parameter("target", "The target configuration file to update")
                .function(map -> {
                    String type = map.get("type");
                    if (type.equalsIgnoreCase("properties")) {
                        try {
                            writeConfig(map.get("content"), map.getOrDefault("target", "application.properties"));
                            return true;
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    return false;
                })
                .build();

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

    public static void updateConfig(final Map<String, String> values) {
        for (Entry<String, String> entry : values.entrySet()) {
            updateConfig(entry.getKey(), entry.getValue(), "", "application.properties");
        }
    }

    static void updateConfig(final String name, final String value, final String profile, final String target) {
        try {
            final Path configPath = getConfigPath(target);
            final String profileName = profile != null && !profile.isEmpty() ? "%" + profile + "." + name : name;
            final int lineNumber = findLineNumber(configPath, profileName);
            List<String> lines = Files.readAllLines(configPath, UTF_8);
            if (lineNumber > 0 && lineNumber < lines.size()) {
                lines.set(lineNumber - 1, profileName + "=" + value);
            } else {
                if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) {
                    lines.add("");
                }
                lines.add(name + "=" + value);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(configPath, UTF_8)) {
                for (String i : lines) {
                    writer.write(i);
                    writer.newLine();
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static void writeConfig(final String content, final String target) throws IOException {
        Path configPath = getConfigPath(target);
        // to validate the content
        new Properties().load(new StringReader(content));
        try (BufferedWriter writer = Files.newBufferedWriter(configPath, UTF_8)) {
            writer.write(content);
            writer.newLine();
        }
    }

    private static Path getConfigPath(final String target) throws IOException {
        assert target != null;
        List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
        if (resourcesDir.isEmpty()) {
            throw new IllegalStateException("Unable to manage configurations - no resource directory found");
        }

        Path path = resourcesDir.get(0);
        Path configPath = path.resolve(Paths.get(target).getFileName());
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath.getParent());
            Files.createFile(configPath);
        }
        return configPath;
    }

    private static int findLineNumber(final Path configPath, final String name) throws IOException {
        PropertiesConfigSource properties = new PropertiesConfigSource(configPath.toUri().toURL());
        ConfigValue fileConfigValue = properties.getConfigValue(name);
        int lineNumber = -1;
        if (fileConfigValue != null) {
            lineNumber = fileConfigValue.getLineNumber();
        }
        return lineNumber;
    }

    private static final String NAMESPACE = "devui-configuration";
}
