package io.quarkus.vertx.http.deployment.devmode.console;

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.arc.runtime.ConfigRecorder;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescriptionsSupplier;
import io.quarkus.vertx.http.runtime.devmode.HasDevServicesSupplier;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class ConfigEditorProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void config(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> devConsoleRuntimeTemplateProducer,
            ConfigRecorder recorder,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {
        List<ConfigDescription> configDescriptions = new ArrayList<>();
        for (ConfigDescriptionBuildItem item : configDescriptionBuildItems) {
            configDescriptions.add(
                    new ConfigDescription(item.getPropertyName(),
                            item.getDocs(),
                            item.getDefaultValue(),
                            isSetByDevServices(devServicesLauncherConfig, item.getPropertyName()),
                            item.getValueTypeName(),
                            item.getAllowedValues(),
                            item.getConfigPhase().name()));
        }

        devConsoleRuntimeTemplateProducer.produce(new DevConsoleRuntimeTemplateInfoBuildItem("config",
                new ConfigDescriptionsSupplier(configDescriptions)));

        devConsoleRuntimeTemplateProducer.produce(new DevConsoleRuntimeTemplateInfoBuildItem("hasDevServices",
                new HasDevServicesSupplier(devServicesLauncherConfig.isPresent()
                        && devServicesLauncherConfig.get().getConfig() != null
                        && !devServicesLauncherConfig.get().getConfig().isEmpty())));
    }

    @BuildStep
    void handleRequests(BuildProducer<DevConsoleRouteBuildItem> devConsoleRouteProducer,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {

        CurrentConfig.EDITOR = ConfigEditorProcessor::updateConfig;

        devConsoleRouteProducer.produce(new DevConsoleRouteBuildItem("config", "POST", new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String action = event.request().getFormAttribute("action");
                if (action.equals("updateProperty")) {
                    String name = event.request().getFormAttribute("name");
                    String value = event.request().getFormAttribute("value");
                    Map<String, String> values = Collections.singletonMap(name, value);

                    updateConfig(values);
                } else if (action.equals("copyTestDevServices") && devServicesLauncherConfig.isPresent()) {
                    Map<String, String> autoconfig = devServicesLauncherConfig.get().getConfig();

                    autoconfig = autoconfig.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> appendProfile("test", e.getKey()),
                                    Map.Entry::getValue));

                    updateConfig(autoconfig);
                } else if (action.equals("copyProdDevServices") && devServicesLauncherConfig.isPresent()) {
                    Map<String, String> autoconfig = devServicesLauncherConfig.get().getConfig();

                    autoconfig = autoconfig.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> appendProfile("prod", e.getKey()),
                                    Map.Entry::getValue));

                    updateConfig(autoconfig);
                } else if (action.equals("updateProperties")) {
                    Map<String, String> properties = new LinkedHashMap<>();
                    String values = event.request().getFormAttribute("values");
                    setConfig(values);
                }
            }
        }));

        devConsoleRouteProducer.produce(new DevConsoleRouteBuildItem("config/all", "GET", (e) -> {
            e.end(Buffer.buffer(getConfig()));
        }));
    }

    private String appendProfile(String profile, String originalKey) {
        return String.format("%%%s.%s", profile, originalKey);
    }

    static byte[] getConfig() {
        try {
            List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
            if (resourcesDir.isEmpty()) {
                throw new IllegalStateException("Unable to manage configurations - no resource directory found");
            }

            // In the current project only
            Path path = resourcesDir.get(0);
            Path configPath = path.resolve("application.properties");
            if (!Files.exists(configPath)) {
                return "".getBytes();
            }

            return Files.readAllBytes(configPath);

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static void updateConfig(Map<String, String> values) {
        try {
            Path configPath = getConfigPath();
            String profile = ProfileManager.getActiveProfile();
            List<String> lines = Files.readAllLines(configPath);
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                name = !profile.equals(DEVELOPMENT.getDefaultProfile()) ? "%" + profile + "." + name : name;
                int nameLine = -1;
                for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
                    final String line = lines.get(i);
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
            preventKill();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static void setConfig(String value) {
        try {
            Path configPath = getConfigPath();
            String profile = ProfileManager.getActiveProfile();
            List<String> lines = Files.readAllLines(configPath);

            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                if (value == null || value.isEmpty()) {
                    writer.newLine();
                } else {
                    writer.write(value);
                }
            }
            preventKill();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static void preventKill() throws Exception {
        //if we don't set this the connection will be killed on restart
        DevConsoleManager.setDoingHttpInitiatedReload(true);
        try {
            DevConsoleManager.getHotReplacementContext().doScan(true);
        } finally {
            DevConsoleManager.setDoingHttpInitiatedReload(false);
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
            configPath = Files.createFile(path.resolve("application.properties"));
        }
        return configPath;
    }

    private boolean isSetByDevServices(Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            String propertyName) {
        if (devServicesLauncherConfig.isPresent()) {
            return devServicesLauncherConfig.get().getConfig().containsKey(propertyName);
        }
        return false;
    }
}
