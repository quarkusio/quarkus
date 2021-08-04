package io.quarkus.vertx.http.deployment.devmode.console;

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.ConfigRecorder;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.devui.DevConsoleManager;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescriptionsSupplier;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class ConfigEditorProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public DevConsoleRuntimeTemplateInfoBuildItem config(ConfigRecorder recorder,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems) {
        List<ConfigDescription> configDescriptions = new ArrayList<>();
        for (ConfigDescriptionBuildItem item : configDescriptionBuildItems) {
            configDescriptions.add(
                    new ConfigDescription(item.getPropertyName(), item.getDocs(), item.getDefaultValue()));
        }
        return new DevConsoleRuntimeTemplateInfoBuildItem("config", new ConfigDescriptionsSupplier(configDescriptions));
    }

    @BuildStep
    DevConsoleRouteBuildItem handlePost() {
        CurrentConfig.EDITOR = ConfigEditorProcessor::updateConfig;
        return new DevConsoleRouteBuildItem("config", "POST", new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String name = event.request().getFormAttribute("name");
                String value = event.request().getFormAttribute("value");
                Map<String, String> values = Collections.singletonMap(name, value);

                updateConfig(values);
                flashMessage(event, "Configuration updated");
            }

        });
    }

    static void updateConfig(Map<String, String> values) {
        try {
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
            //if we don't set this the connection will be killed on restart
            DevConsoleManager.setDoingHttpInitiatedReload(true);
            try {
                DevConsoleManager.getHotReplacementContext().doScan(true);
            } finally {
                DevConsoleManager.setDoingHttpInitiatedReload(false);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
