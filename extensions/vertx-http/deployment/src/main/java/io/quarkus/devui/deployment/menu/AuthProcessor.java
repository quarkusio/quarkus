package io.quarkus.devui.deployment.menu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.config.*;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.smallrye.config.SmallRyeConfig;

/**
 * This creates Extensions Page
 */
public class AuthProcessor {

    private static final String NAME_SPACE = "devui-auth";
    private static List<ConfigDescriptionBuildItem> configDescriptionBuildItems;
    private static Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig;
    private static AuthDevUIRecorder recorder;
    private static SmallRyeConfig current;

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createAuthPages() {

        InternalPageBuildItem configurationPages = new InternalPageBuildItem("Auth", 21);

        configurationPages.addPage(Page.webComponentPageBuilder()
                .namespace(NAME_SPACE)
                .title("Permissions")
                .icon("font-awesome-solid:shield")
                .componentLink("qwc-auth.js"));

        return configurationPages;
    }

    private Optional<ConfigDescriptionBuildItem> findByName(String propertyKey,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems) {
        Pattern pattern = PermissionDescription.getPattern(propertyKey);

        for (ConfigDescriptionBuildItem configDescriptionBuildItem : configDescriptionBuildItems) {
            boolean matches = configDescriptionBuildItem.getPropertyName().matches(pattern.pattern());
            if (matches) {
                return Optional.of(configDescriptionBuildItem);
            }

        }
        return Optional.empty();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerConfigs(List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            AuthDevUIRecorder recorder) {

        AuthProcessor.configDescriptionBuildItems = configDescriptionBuildItems;
        AuthProcessor.devServicesLauncherConfig = devServicesLauncherConfig;
        AuthProcessor.recorder = recorder;
        AuthProcessor.current = (SmallRyeConfig) ConfigProvider.getConfig();

        register(configDescriptionBuildItems, devServicesLauncherConfig, recorder);
    }

    private void register(List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig, AuthDevUIRecorder recorder) {
        List<PermissionDescription> configDescriptions = new ArrayList<>();

        for (ConfigDescriptionBuildItem configDescriptionBuildItem : configDescriptionBuildItems) {
            if (configDescriptionBuildItem.getPropertyName().contains("permission.new123"))
                System.out.println("configDescriptionBuildItem = " + configDescriptionBuildItem.getPropertyName() + " default: "
                        + configDescriptionBuildItem.getDefaultValue());
        }

        for (ConfigSource configSource : current.getConfigSources()) {

            for (String propertyKey : configSource.getPropertyNames()) {
                var isPermission = propertyKey.startsWith("quarkus.http.auth.permission");
                var isPolicy = propertyKey.startsWith("quarkus.http.auth.policy");

                if (!(isPermission || isPolicy))
                    continue;

                Optional<ConfigDescriptionBuildItem> optionalConfigDescriptionBuildItem = this.findByName(propertyKey,
                        configDescriptionBuildItems);

                if (optionalConfigDescriptionBuildItem.isPresent()) {
                    var item = optionalConfigDescriptionBuildItem.get();
                    PermissionDescription permissionDescription = new PermissionDescription(
                            propertyKey,
                            configSource.getValue(propertyKey),
                            new ConfigDescription(item.getPropertyName(),
                                    formatJavadoc(cleanUpAsciiDocIfNecessary(item.getDocs())),
                                    item.getDefaultValue(),
                                    isSetByDevServices(devServicesLauncherConfig, item.getPropertyName()),
                                    item.getValueTypeName(),
                                    item.getAllowedValues(),
                                    item.getConfigPhase().name()));
                    configDescriptions.add(permissionDescription);
                }
            }
        }

        Map<AuthFieldType.AuthConfigType, List<PermissionSet>> result = configDescriptions.stream()
                .collect(Collectors.groupingBy(
                        PermissionDescription::getConfigType,
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(PermissionDescription::getConfigId,
                                        Collectors.mapping(d -> d, Collectors.toList())),
                                configMap -> configMap.values().stream()
                                        .map(PermissionSet::new)
                                        .sorted()
                                        .collect(Collectors.toList()))));

        List<PermissionSet> permissionSets = result.get(AuthFieldType.AuthConfigType.PERMISSION);
        List<PermissionSet> policyGroups = result.get(AuthFieldType.AuthConfigType.POLICY);

        recorder.registerPermissionGroups(permissionSets, policyGroups);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerJsonRpcService(
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProvidersProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            AuthDevUIRecorder recorder) {

        DevConsoleManager.register("update-permission-set", map -> {
            updateConfig(map);
            register(AuthProcessor.configDescriptionBuildItems, AuthProcessor.devServicesLauncherConfig,
                    AuthProcessor.recorder);
            return null;
        });

        syntheticBeanProducer.produce(
                SyntheticBeanBuildItem.configure(AuthDescriptionBean.class).unremovable()
                        .supplier(recorder.permissionGroupBean())
                        .scope(Singleton.class)
                        .setRuntimeInit()
                        .done());

        jsonRPCProvidersProducer.produce(new JsonRPCProvidersBuildItem(NAME_SPACE, AuthJsonRPCService.class));
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
}
