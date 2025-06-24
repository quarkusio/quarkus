package io.quarkus.vertx.http.deployment.console;

import static io.quarkus.devui.deployment.ide.IdeProcessor.openBrowser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescriptionsManager;

public class ConsoleProcessor {

    static volatile ConsoleStateManager.ConsoleContext context;

    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    void setupConsole(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return;
        }
        if (context == null) {
            context = ConsoleStateManager.INSTANCE.createContext("HTTP");
        }
        Config c = ConfigProvider.getConfig();
        String host = c.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
        boolean isInsecureDisabled = c.getOptionalValue("quarkus.http.insecure-requests", String.class)
                .map("disabled"::equals)
                .orElse(false);

        String port = isInsecureDisabled
                ? c.getOptionalValue("quarkus.http.ssl-port", String.class).orElse("8443")
                : c.getOptionalValue("quarkus.http.port", String.class).orElse("8080");

        String protocol = isInsecureDisabled ? "https" : "http";

        context.reset(
                new ConsoleCommand('w', "Open the application in a browser", null,
                        () -> openBrowser(rp, np, protocol, "/", host, port)),
                new ConsoleCommand('d', "Open the Dev UI in a browser", null,
                        () -> openBrowser(rp, np, protocol, "/q/dev-ui", host, port)));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void config(List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            BuildProducer<ConsoleCommandBuildItem> consoleCommandBuildItemBuildProducer,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig) {
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

        consoleCommandBuildItemBuildProducer.produce(
                new ConsoleCommandBuildItem(new ConfigCommandGroup(new ConfigDescriptionsManager(configDescriptions))));

    }

    public static boolean isSetByDevServices(Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            String propertyName) {
        if (devServicesLauncherConfig.isPresent()) {
            return devServicesLauncherConfig.get().getConfig().containsKey(propertyName);
        }
        return false;
    }

    public static String cleanUpAsciiDocIfNecessary(String docs) {
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
