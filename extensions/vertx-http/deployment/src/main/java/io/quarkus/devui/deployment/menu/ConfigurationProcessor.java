package io.quarkus.devui.deployment.menu;

import static io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor.cleanUpAsciiDocIfNecessary;
import static io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor.isSetByDevServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;

/**
 * This creates Extensions Page
 */
public class ConfigurationProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createConfigurationPages(List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
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

        configurationPages.addBuildTimeData("allConfiguration",
                getAllConfig(configDescriptionBuildItems, devServicesLauncherConfig));

        return configurationPages;
    }

    private List<ConfigDescription> getAllConfig(List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {
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
        return configDescriptions;
    }

    private static final Pattern codePattern = Pattern.compile("(\\{@code )([^}]+)(\\})");
    private static final Pattern linkPattern = Pattern.compile("(\\{@link )([^}]+)(\\})");

    static String formatJavadoc(String val) {
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
