package io.quarkus.devui.deployment.menu;

import static io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor.cleanUpAsciiDocIfNecessary;
import static io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor.isSetByDevServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

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
import io.quarkus.devui.runtime.config.ConfigDescriptionBean;
import io.quarkus.devui.runtime.config.ConfigDevUiRecorder;
import io.quarkus.devui.runtime.config.ConfigJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;

/**
 * This creates Extensions Page
 */
public class ConfigurationProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createConfigurationPages(
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {

        InternalPageBuildItem configurationPages = new InternalPageBuildItem("Configuration", 20);

        configurationPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-configuration")
                .title("Form Editor")
                .icon("font-awesome-solid:sliders")
                .componentLink("qwc-configuration.js"));

        configurationPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-configuration")
                .title("Source Editor")
                .icon("font-awesome-solid:code")
                .componentLink("qwc-configuration-editor.js"));

        configurationPages.addBuildTimeData("allConfiguration", new ArrayList<ConfigDescription>());

        return configurationPages;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    void registerConfigs(List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            ConfigDevUiRecorder recorder) {

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

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerJsonRpcService(
            BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProvidersProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            ConfigDevUiRecorder recorder) {

        DevConsoleManager.register("config-update-property", map -> {
            Map<String, String> values = Collections.singletonMap(map.get("name"), map.get("value"));
            ConfigEditorProcessor.updateConfig(values, false);
            return null;
        });
        DevConsoleManager.register("config-set-properties", value -> {
            String content = value.get("content");
            ConfigEditorProcessor.setConfig(content, false);
            return null;
        });

        syntheticBeanProducer.produce(
                SyntheticBeanBuildItem.configure(ConfigDescriptionBean.class).unremovable()
                        .supplier(recorder.configDescriptionBean())
                        .scope(Singleton.class)
                        .setRuntimeInit()
                        .done());

        jsonRPCProvidersProducer.produce(new JsonRPCProvidersBuildItem("devui-configuration", ConfigJsonRPCService.class));
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
}
