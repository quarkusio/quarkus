package io.quarkus.devui.deployment.menu;

import java.io.IOException;
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
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.config.ApplicationPropertiesService;
import io.quarkus.devui.runtime.config.ConfigDescription;
import io.quarkus.devui.runtime.config.ConfigDescriptionBean;
import io.quarkus.devui.runtime.config.ConfigDevUIRecorder;
import io.quarkus.devui.runtime.config.ConfigJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Extensions Page
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ConfigurationProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
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

    @BuildStep(onlyIf = IsDevelopment.class)
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
                            devServicesLauncherConfig
                                    .map(DevServicesLauncherConfigResultBuildItem::getConfig)
                                    .map(config -> config.containsKey(item.getPropertyName()))
                                    .orElse(false),
                            item.getValueTypeName(),
                            item.getAllowedValues(),
                            item.getConfigPhase().name()));
        }

        Set<String> devServicesConfig = new HashSet<>();
        devServicesLauncherConfig.ifPresent(
                devServicesLauncherConfigResultBuildItem -> devServicesConfig
                        .addAll(devServicesLauncherConfigResultBuildItem.getConfig().keySet()));

        recorder.registerConfigs(configDescriptions, devServicesConfig);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerJsonRpcService(
            BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProvidersProducer,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            ConfigDevUIRecorder recorder,
            CuratedApplicationShutdownBuildItem shutdown) {

        BuildTimeActionBuildItem configActions = new BuildTimeActionBuildItem(NAMESPACE);

        configActions.addAction("updateProperty", payload -> {
            final var updates = new Properties();
            updates.setProperty(
                    payload.get("name"),
                    Optional
                            .ofNullable(payload.get("value"))
                            .orElse(""));
            try {
                new ApplicationPropertiesService()
                        .mergeApplicationProperties(updates);
            } catch (IOException e) {
                return false;
            }
            return true;
        });
        configActions.addAction("updatePropertiesAsString", payload -> {
            if ("properties".equalsIgnoreCase(payload.get("type"))) {
                final var content = payload.get("content");
                try {
                    new ApplicationPropertiesService()
                            .saveApplicationProperties(content);
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
        syntheticBeanProducer.produce(
                SyntheticBeanBuildItem.configure(ApplicationPropertiesService.class).unremovable()
                        .supplier(recorder.applicationPropertiesService())
                        .scope(Singleton.class)
                        .setRuntimeInit()
                        .done());

        ConfigurationProcessor.setDefaultConfigEditor();
        shutdown.addCloseTask(() -> {
            CurrentConfig.EDITOR = null;
            CurrentConfig.CURRENT = Collections.emptyList();
        }, true);

        jsonRPCProvidersProducer.produce(new JsonRPCProvidersBuildItem(NAMESPACE, ConfigJsonRPCService.class));
    }

    public static void setDefaultConfigEditor() {
        CurrentConfig.EDITOR = ConfigurationProcessor::mergeApplicationProperties;
    }

    private static void mergeApplicationProperties(Map<String, String> updatesMap) {
        final var updates = new Properties();
        updates.putAll(updatesMap);
        try {
            new ApplicationPropertiesService()
                    .mergeApplicationProperties(updates);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private static final String NAMESPACE = "devui-configuration";
}
