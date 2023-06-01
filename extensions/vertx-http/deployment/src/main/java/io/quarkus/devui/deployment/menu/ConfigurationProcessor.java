package io.quarkus.devui.deployment.menu;

import static io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor.cleanUpAsciiDocIfNecessary;
import static io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor.isSetByDevServices;
import static io.smallrye.config.Expressions.withoutExpansion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.config.ConfigJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.devmode.console.ConfigEditorProcessor;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

/**
 * This creates Extensions Page
 */
public class ConfigurationProcessor {

    private static final String QUOTED_DOT = "\".\"";
    private static final String QUOTED_DOT_KEY = "$$QUOTED_DOT$$";

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

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcService() {
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
        return new JsonRPCProvidersBuildItem("devui-configuration", ConfigJsonRPCService.class);
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

        Set<String> devServicesConfig = new HashSet<>();
        if (devServicesLauncherConfig.isPresent()) {
            devServicesConfig.addAll(devServicesLauncherConfig.get().getConfig().keySet());
        }

        return calculate(configDescriptions, devServicesConfig);
    }

    private List<ConfigDescription> calculate(List<ConfigDescription> cd, Set<String> devServicesProperties) {
        List<ConfigDescription> configDescriptions = new ArrayList<>(cd);

        List<ConfigDescription> ordered = new ArrayList<>();
        List<String> properties = new ArrayList<>();
        SmallRyeConfig current = (SmallRyeConfig) ConfigProvider.getConfig();

        Map<List<String>, Set<String>> allPropertySegments = new HashMap<>();
        Set<String> propertyNames = new HashSet<>();
        current.getPropertyNames().forEach(propertyNames::add);
        for (String propertyName : propertyNames) {
            propertyName = propertyName.replace(QUOTED_DOT, QUOTED_DOT_KEY); // Make sure dots can be quoted
            String[] parts = propertyName.split("\\.");

            List<String> accumulate = new ArrayList<>();
            //we never want to add the full string
            //hence -1
            for (int i = 0; i < parts.length - 1; ++i) {
                if (parts[i].isEmpty()) {
                    //this can't map to a quarkus prop as it has an empty segment
                    //so skip
                    break;
                }
                // If there was a quoted dot, put that back
                if (parts[i].contains(QUOTED_DOT_KEY)) {
                    parts[i] = parts[i].replaceAll(QUOTED_DOT_KEY, QUOTED_DOT);
                }

                accumulate.add(parts[i]);
                //if there is both a quoted and unquoted version we only want to apply the quoted version
                //and remove the unquoted one
                Set<String> potentialSegmentSet = allPropertySegments.computeIfAbsent(List.copyOf(accumulate),
                        (k) -> new HashSet<>());
                if (isQuoted(parts[i + 1])) {
                    potentialSegmentSet.add(parts[i + 1]);
                    potentialSegmentSet.remove(parts[i + 1].substring(1, parts[i + 1].length() - 1));
                } else {
                    if (!potentialSegmentSet.contains(ensureQuoted(parts[i + 1]))) {
                        potentialSegmentSet.add(parts[i + 1]);
                    }
                }

            }
        }

        Map<List<String>, Set<String>> wildcardsToAdd = new HashMap<>();
        Map<String, Holder> foundItems = new HashMap<>();
        Set<String> bannedExpansionCombos = new HashSet<>();
        //we iterate over every config description
        for (ConfigDescription item : configDescriptions) {
            //if they are a non-wildcard description we just add them directly
            if (!item.getName().contains("{*}")) {
                //we don't want to accidentally use these properties as name expansions
                //we ban them which means that the only way the name can be expanded into a map
                //is if it is quoted
                bannedExpansionCombos.add(item.getName());
                for (int i = 0; i < item.getName().length(); ++i) {
                    //add all possible segments to the banned list
                    if (item.getName().charAt(i) == '.') {
                        bannedExpansionCombos.add(item.getName().substring(0, i));
                    }
                }
                properties.add(item.getName());
                item.setConfigValue(getConfigValue(current, item.getName()));
                ordered.add(item);
            } else if (!item.getName().startsWith("quarkus.log.filter")) { //special case, we use this internally and we don't want it clogging up the editor
                //we need to figure out how to expand it
                //this can have multiple stars
                List<List<String>> componentParts = new ArrayList<>();
                List<String> accumulator = new ArrayList<>();
                //keys that were used to expand, checked against the banned list before adding
                for (var i : item.getName().split("\\.")) {
                    if (i.equals("{*}")) {
                        componentParts.add(accumulator);
                        accumulator = new ArrayList<>();
                    } else {
                        accumulator.add(i);
                    }
                }
                //note that accumulator is still holding the final part
                //we need it later, but we don't want it in this loop
                Map<List<String>, Set<String>> building = new HashMap<>();
                building.put(List.of(), new HashSet<>());
                for (List<String> currentPart : componentParts) {
                    Map<List<String>, Set<String>> newBuilding = new HashMap<>();
                    for (Map.Entry<List<String>, Set<String>> entry : building.entrySet()) {
                        List<String> attempt = entry.getKey();
                        List<String> newBase = new ArrayList<>(attempt);
                        newBase.addAll(currentPart);
                        wildcardsToAdd.put(newBase, entry.getValue());
                        Set<String> potential = allPropertySegments.get(newBase);
                        if (potential != null) {
                            bannedExpansionCombos.add(String.join(".", newBase).replace("\"", ""));
                            for (String definedName : potential) {
                                List<String> toAdd = new ArrayList<>(newBase);
                                toAdd.add(definedName);
                                //for expansion keys we always use unquoted values, same with banned
                                //so we are always comparing unquoted
                                Set<String> expansionKeys = new HashSet<>(entry.getValue());
                                expansionKeys.add(String.join(".", newBase) + "." + definedName);
                                newBuilding.put(toAdd, expansionKeys);
                            }
                        }
                    }
                    building = newBuilding;
                }
                //now we have our config properties
                for (var entry : building.entrySet()) {
                    List<String> segments = entry.getKey();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < segments.size(); ++i) {
                        if (i > 0) {
                            sb.append(".");
                        }
                        sb.append(segments.get(i));
                    }
                    //accumulator holds the find string
                    for (String s : accumulator) {
                        sb.append(".").append(s);
                    }
                    String expandedName = sb.toString();
                    foundItems.put(expandedName, new Holder(entry.getValue(), item));
                }
            }
        }
        for (Map.Entry<String, Holder> e : foundItems.entrySet()) {
            boolean ok = true;
            for (String key : e.getValue().expansionKeys) {
                if (bannedExpansionCombos.contains(key)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }
            String expandedName = e.getKey();
            var item = e.getValue().configDescription;
            ConfigDescription newDesc = new ConfigDescription(expandedName, item.getDescription(),
                    item.getDefaultValue(), devServicesProperties.contains(expandedName), item.getTypeName(),
                    item.getAllowedValues(),
                    item.getConfigPhase());

            properties.add(newDesc.getName());
            newDesc.setConfigValue(getConfigValue(current, newDesc.getName()));
            ordered.add(newDesc);
        }

        //now add our star properties
        for (var entry : wildcardsToAdd.entrySet()) {
            boolean ok = true;
            for (String key : entry.getValue()) {
                if (bannedExpansionCombos.contains(key)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }
            List<String> segments = entry.getKey();
            StringBuilder sb = new StringBuilder();
            for (String segment : segments) {
                sb.append(segment);
                sb.append(".");
            }
            String expandedName = sb.toString();
            ConfigDescription newDesc = new ConfigDescription(expandedName, true);

            properties.add(newDesc.getName());
            newDesc.setConfigValue(getConfigValue(current, newDesc.getName()));
            ordered.add(newDesc);
        }

        for (ConfigSource configSource : current.getConfigSources()) {
            if (configSource.getName().equals("PropertiesConfigSource[source=Build system]")) {
                properties.addAll(configSource.getPropertyNames());
            }
        }

        withoutExpansion(() -> {
            for (String propertyName : current.getPropertyNames()) {
                if (properties.contains(propertyName)) {
                    continue;
                }

                ConfigDescription item = new ConfigDescription(propertyName, null, null, getConfigValue(current, propertyName));
                ordered.add(item);

                configDescriptions.add(item);
            }
        });

        return ordered;
    }

    private ConfigValue getConfigValue(SmallRyeConfig config, String name) {
        try {
            return config.getConfigValue(name);
        } catch (java.util.NoSuchElementException nse) {
            return null;
        }
    }

    private String ensureQuoted(String part) {
        if (isQuoted(part)) {
            return part;
        }
        return "\"" + part + "\"";
    }

    private boolean isQuoted(String part) {
        return part.length() >= 2 && part.charAt(0) == '\"' && part.charAt(part.length() - 1) == '\"';
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

    static class Holder {
        final Set<String> expansionKeys;
        final ConfigDescription configDescription;

        private Holder(Set<String> expansionKeys, ConfigDescription configDescription) {
            this.expansionKeys = expansionKeys;
            this.configDescription = configDescription;
        }
    }
}
