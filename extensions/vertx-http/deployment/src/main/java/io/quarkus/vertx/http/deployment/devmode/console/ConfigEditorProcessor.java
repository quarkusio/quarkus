package io.quarkus.vertx.http.deployment.devmode.console;

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.validator.CommandValidator;
import org.aesh.command.validator.CommandValidatorException;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescriptionsManager;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescriptionsRecorder;
import io.quarkus.vertx.http.runtime.devmode.HasDevServicesSupplier;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class ConfigEditorProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void config(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> devConsoleRuntimeTemplateProducer,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            BuildProducer<ConsoleCommandBuildItem> consoleCommandBuildItemBuildProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem, BuildProducer<DevConsoleRouteBuildItem> devConsoleRouteProducer,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig, ConfigDescriptionsRecorder recorder) {
        List<ConfigDescription> configDescriptions = new ArrayList<>();
        for (ConfigDescriptionBuildItem item : configDescriptionBuildItems) {
            configDescriptions.add(
                    new ConfigDescription(item.getPropertyName(),
                            cleanUpAsciiDocIfNecessary(item.getDocs()),
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

        ConfigDescriptionsManager configDescriptionsManager = recorder.manager(configDescriptions, devServicesConfig);

        consoleCommandBuildItemBuildProducer.produce(
                new ConsoleCommandBuildItem(new ConfigCommandGroup(new ConfigDescriptionsManager(configDescriptions))));

        devConsoleRuntimeTemplateProducer.produce(new DevConsoleRuntimeTemplateInfoBuildItem("config",
                configDescriptionsManager, this.getClass(), curateOutcomeBuildItem));

        devConsoleRuntimeTemplateProducer.produce(new DevConsoleRuntimeTemplateInfoBuildItem("hasDevServices",
                new HasDevServicesSupplier(devServicesLauncherConfig.isPresent()
                        && devServicesLauncherConfig.get().getConfig() != null
                        && !devServicesLauncherConfig.get().getConfig().isEmpty()),
                this.getClass(),
                curateOutcomeBuildItem));

        devConsoleRouteProducer.produce(new DevConsoleRouteBuildItem("add-named-group", "POST", configDescriptionsManager));
        devConsoleRouteProducer.produce(new DevConsoleRouteBuildItem("config", "POST", new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String action = event.request().getFormAttribute("action");
                if (action.equals("updateProperty")) {
                    String name = event.request().getFormAttribute("name");
                    String value = event.request().getFormAttribute("value");
                    String wildcard = event.request().getFormAttribute("wildcard");
                    if (!"true".equals(wildcard)) {
                        Map<String, String> values = Collections.singletonMap(name, value);
                        updateConfig(values);
                    } else {
                        String newProp = name + value;
                        configDescriptionsManager.addNamedConfigGroup(newProp);
                    }
                } else if (action.equals("copyDevServices") && devServicesLauncherConfig.isPresent()) {
                    String environment = event.request().getFormAttribute("environment");
                    String filter = event.request().getParam("filterConfigKeys");
                    List<String> configFilter = getConfigFilter(filter);
                    Map<String, String> autoconfig = devServicesLauncherConfig.get().getConfig();

                    autoconfig = filterAndApplyProfile(autoconfig, configFilter, environment.toLowerCase());

                    updateConfig(autoconfig);
                } else if (action.equals("updateProperties")) {
                    Map<String, String> properties = new LinkedHashMap<>();
                    String values = event.request().getFormAttribute("values");
                    setConfig(values);
                }
            }
        }));

    }

    private String cleanUpAsciiDocIfNecessary(String docs) {
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

    @BuildStep
    void handleRequests(BuildProducer<DevConsoleRouteBuildItem> devConsoleRouteProducer,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {

        CurrentConfig.EDITOR = ConfigEditorProcessor::updateConfig;

        devConsoleRouteProducer.produce(new DevConsoleRouteBuildItem("config/all", "GET", (e) -> {
            e.end(Buffer.buffer(getConfig()));
        }));
    }

    private Map<String, String> filterAndApplyProfile(Map<String, String> autoconfig, List<String> configFilter,
            String profile) {
        return autoconfig.entrySet().stream()
                .filter((t) -> {
                    if (configFilter != null && !configFilter.isEmpty()) {
                        for (String sw : configFilter) {
                            if (t.getKey().startsWith(sw)) {
                                return true;
                            }
                        }
                    } else {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toMap(
                        e -> appendProfile(profile, e.getKey()),
                        Map.Entry::getValue));
    }

    private List<String> getConfigFilter(String filter) {
        if (filter != null && !filter.isEmpty()) {
            if (filter.contains(",")) {
                return Arrays.asList(filter.split(","));
            } else {
                return List.of(filter);
            }
        }
        return Collections.EMPTY_LIST;
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
        if (values != null && !values.isEmpty()) {
            try {
                Path configPath = getConfigPath();
                List<String> profiles = ConfigUtils.getProfiles();
                List<String> lines = Files.readAllLines(configPath);
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    int nameLine = -1;
                    for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
                        String line = lines.get(i);
                        for (String profile : profiles) {
                            String profileName = !profile.equals(DEVELOPMENT.getDefaultProfile()) ? "%" + profile + "." + name
                                    : name;
                            if (line.startsWith(profileName + "=")) {
                                name = profileName;
                                nameLine = i;
                                break;
                            }
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
    }

    static void setConfig(String value) {
        try {
            Path configPath = getConfigPath();
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
            Files.createDirectories(configPath.getParent());
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

    @GroupCommandDefinition(name = "config", description = "Config Editing Commands")
    public static class ConfigCommandGroup implements GroupCommand {

        final ConfigDescriptionsManager configDescriptionsManager;

        @Option(shortName = 'h', hasValue = false, overrideRequired = true)
        public boolean help;

        public ConfigCommandGroup(ConfigDescriptionsManager configDescriptionsManager) {
            this.configDescriptionsManager = configDescriptionsManager;
        }

        @Override
        public List<Command> getCommands() {
            return List.of(new SetConfigCommand(configDescriptionsManager));
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.getShell().writeln(commandInvocation.getHelpInfo());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "set", description = "Sets a config value", validator = SetValidator.class)
    public static class SetConfigCommand implements Command<CommandInvocation> {

        final ConfigDescriptionsManager configDescriptionsManager;

        @Argument(required = true, completer = SetConfigCompleter.class)
        private String command;

        public SetConfigCommand(ConfigDescriptionsManager configDescriptionsManager) {
            this.configDescriptionsManager = configDescriptionsManager;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            int pos = command.indexOf('=');
            String key = command.substring(0, pos);
            String value = command.substring(pos + 1);
            CurrentConfig.EDITOR.accept(Map.of(key, value));
            return CommandResult.SUCCESS;
        }
    }

    public static class SetConfigCompleter implements OptionCompleter<CompleterInvocation> {

        @Override
        public void complete(CompleterInvocation completerInvocation) {
            String soFar = completerInvocation.getGivenCompleteValue();
            String configKey = null;
            if (soFar.indexOf('=') != -1) {
                configKey = soFar.substring(0, soFar.indexOf('='));
            }
            SetConfigCommand command = (SetConfigCommand) completerInvocation.getCommand();
            Set<String> possible = new HashSet<>();
            for (var j : command.configDescriptionsManager.values().values()) {
                for (var i : j) {
                    if (i.getDescription() == null) {
                        continue;
                    }
                    //we have found the entry for the selected key
                    if (configKey != null && configKey.equals(i.getName())) {
                        if (i.getAllowedValues() != null && !i.getAllowedValues().isEmpty()) {
                            for (String val : i.getAllowedValues()) {
                                String value = i.getName() + "=" + val;
                                if (value.startsWith(soFar)) {
                                    completerInvocation.addCompleterValue(value);
                                }
                            }
                        }
                        return;
                    }
                    if (i.isWildcardEntry()) {
                        continue;
                    }
                    if (i.getName().equals(soFar)) {
                        possible.add(soFar + "=");
                    } else if (i.getName().startsWith(soFar)) {
                        //we just want to complete the next segment
                        int pos = i.getName().indexOf('.', soFar.length() + 1);
                        if (pos == -1) {
                            possible.add(i.getName() + "=");
                        } else {
                            possible.add(i.getName().substring(0, pos) + ".");
                        }
                    }
                }
            }
            completerInvocation.setAppendSpace(false);
            completerInvocation.addAllCompleterValues(possible);

        }
    }

    public static class SetValidator implements CommandValidator<SetConfigCommand, CommandInvocation> {

        @Override
        public void validate(SetConfigCommand command) throws CommandValidatorException {
            //-1 because the last char can't be equals
            for (int i = 0; i < command.command.length() - 1; ++i) {
                if (command.command.charAt(i) == '=') {
                    return;
                }
            }
            throw new CommandValidatorException("Set command must be in the form key=value");
        }
    }

}
