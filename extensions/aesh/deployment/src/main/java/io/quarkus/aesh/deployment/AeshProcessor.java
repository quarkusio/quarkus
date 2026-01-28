package io.quarkus.aesh.deployment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.aesh.runtime.AeshCommandMetadata;
import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.aesh.runtime.AeshMode;
import io.quarkus.aesh.runtime.AeshRecorder;
import io.quarkus.aesh.runtime.AeshRemoteConnectionHandler;
import io.quarkus.aesh.runtime.AeshRunner;
import io.quarkus.aesh.runtime.CliRunner;
import io.quarkus.aesh.runtime.DefaultAeshRuntimeRunnerFactory;
import io.quarkus.aesh.runtime.DefaultCliCommandRegistryFactory;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.runtime.annotations.QuarkusMain;

class AeshProcessor {

    private static final DotName AESH_COMMAND = DotName.createSimple("org.aesh.command.Command");
    private static final DotName COMMAND_DEFINITION = DotName.createSimple("org.aesh.command.CommandDefinition");
    private static final DotName QUARKUS_MAIN = DotName.createSimple(QuarkusMain.class.getName());
    private static final DotName PARENT_COMMAND = DotName.createSimple("org.aesh.command.option.ParentCommand");

    @BuildStep
    FeatureBuildItem feature(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.PICOCLI)) {
            throw new IllegalStateException(
                    "The Aesh and Picocli extensions cannot be used together. "
                            + "Remove either quarkus-aesh or quarkus-picocli from your dependencies.");
        }
        return new FeatureBuildItem(Feature.AESH);
    }

    /**
     * Discovers command classes from the application index only.
     * Only commands defined in the user's application are considered --
     * commands from third-party dependencies are not automatically registered.
     */
    @BuildStep
    void discoverCommands(ApplicationIndexBuildItem applicationIndex,
            BuildProducer<AeshCommandBuildItem> commands) {
        IndexView appIndex = applicationIndex.getIndex();
        Set<DotName> discovered = new HashSet<>();
        discoverFromIndex(appIndex, discovered, commands);
    }

    private void discoverFromIndex(IndexView index, Set<DotName> discovered,
            BuildProducer<AeshCommandBuildItem> commands) {
        // Process @CommandDefinition classes (which may also define group commands
        // via the groupCommands attribute, added in aesh 3.10)
        for (AnnotationInstance ann : index.getAnnotations(COMMAND_DEFINITION)) {
            if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo classInfo = ann.target().asClass();
            DotName className = classInfo.name();
            if (!discovered.add(className)) {
                continue;
            }

            String commandName = getAnnotationStringValue(ann, "name", "");
            String description = getAnnotationStringValue(ann, "description", "");
            List<String> subCommandClassNames = extractGroupSubCommands(ann);
            boolean isGroup = !subCommandClassNames.isEmpty();

            commands.produce(new AeshCommandBuildItem(
                    className.toString(), commandName, description,
                    isGroup, subCommandClassNames));
        }
    }

    /**
     * Validates discovered commands at build time.
     * Catches configuration errors early with clear, actionable messages
     * instead of letting them surface as cryptic runtime errors.
     */
    @BuildStep
    void validateCommands(List<AeshCommandBuildItem> commands,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        List<Throwable> errors = new ArrayList<>();

        // Collect sub-command class names so we can exclude them from top-level duplicate checks
        Set<String> subCommandClasses = commands.stream()
                .filter(AeshCommandBuildItem::isGroupCommand)
                .flatMap(c -> c.getSubCommandClassNames().stream())
                .collect(Collectors.toSet());

        // 1. Duplicate command names among top-level commands
        Map<String, List<AeshCommandBuildItem>> byName = commands.stream()
                .filter(c -> !c.getCommandName().isEmpty())
                .filter(c -> !subCommandClasses.contains(c.getClassName()))
                .collect(Collectors.groupingBy(AeshCommandBuildItem::getCommandName));
        for (Map.Entry<String, List<AeshCommandBuildItem>> entry : byName.entrySet()) {
            if (entry.getValue().size() > 1) {
                String classes = entry.getValue().stream()
                        .map(AeshCommandBuildItem::getClassName)
                        .collect(Collectors.joining(", "));
                errors.add(new IllegalStateException(
                        "Duplicate command name '" + entry.getKey()
                                + "' found on multiple top-level command classes: " + classes));
            }
        }

        // 2. Group sub-command class missing @CommandDefinition
        IndexView index = combinedIndex.getIndex();
        for (AeshCommandBuildItem cmd : commands) {
            if (!cmd.isGroupCommand()) {
                continue;
            }
            for (String subClassName : cmd.getSubCommandClassNames()) {
                ClassInfo subClass = index.getClassByName(DotName.createSimple(subClassName));
                if (subClass == null) {
                    errors.add(new IllegalStateException(
                            "Group command '" + cmd.getClassName()
                                    + "' references sub-command class '" + subClassName
                                    + "' which was not found in the Jandex index"));
                } else if (!subClass.hasAnnotation(COMMAND_DEFINITION)) {
                    errors.add(new IllegalStateException(
                            "Group command '" + cmd.getClassName()
                                    + "' references sub-command class '" + subClassName
                                    + "' which is not annotated with @CommandDefinition"));
                }
            }
        }

        if (!errors.isEmpty()) {
            validationErrors.produce(new ValidationErrorBuildItem(errors));
        }
    }

    /**
     * Registers discovered commands as CDI beans.
     */
    @BuildStep
    void registerCommandBeans(List<AeshCommandBuildItem> commands,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        for (AeshCommandBuildItem cmd : commands) {
            additionalBean.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(cmd.getClassName())
                    .setDefaultScope(BuiltinScope.DEPENDENT.getName())
                    .setUnremovable()
                    .build());
        }
    }

    @BuildStep
    void addScopeToCommands(BuildProducer<AutoAddScopeBuildItem> autoAddScope) {
        // Add @Dependent to all classes that implement org.aesh.command.Command and:
        // (a) require container services
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .implementsInterface(AESH_COMMAND)
                .requiresContainerServices()
                .defaultScope(BuiltinScope.DEPENDENT)
                .priority(20)
                .unremovable()
                .build());
        // (b) or declare a single constructor with at least one parameter
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .match((clazz, annotations, index) -> {
                    List<MethodInfo> constructors = clazz.methods().stream()
                            .filter(m -> m.name().equals(MethodDescriptor.INIT))
                            .collect(Collectors.toList());
                    return constructors.size() == 1 && constructors.get(0).parametersCount() > 0;
                })
                .implementsInterface(AESH_COMMAND)
                .defaultScope(BuiltinScope.DEPENDENT)
                .priority(10)
                .unremovable()
                .build());
        // Add @Dependent to ALL classes annotated with @CommandDefinition
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(COMMAND_DEFINITION)
                .defaultScope(BuiltinScope.DEPENDENT)
                .priority(20)
                .unremovable()
                .build());
        // Add @Dependent to commands that have a @ParentCommand field (aesh 3.0 hierarchical commands)
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .match((clazz, annotations, index) -> clazz.fields().stream()
                        .anyMatch(f -> f.hasAnnotation(PARENT_COMMAND)))
                .implementsInterface(AESH_COMMAND)
                .defaultScope(BuiltinScope.DEPENDENT)
                .priority(25)
                .unremovable()
                .build());
    }

    @BuildStep
    void registerAeshBeans(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean) {
        // Register aesh annotations as bean-defining annotations so that
        // classes annotated with them are automatically discovered as CDI beans
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(COMMAND_DEFINITION,
                BuiltinScope.DEPENDENT.getName()));

        // Prevent Arc from removing user implementations of aesh interfaces.
        // These are typically referenced only via annotation attributes (e.g. @Option(completer = ...))
        // and not injected directly, so Arc would consider them unused.
        unremovableBean.produce(UnremovableBeanBuildItem.beanTypes(
                DotName.createSimple("org.aesh.command.completer.OptionCompleter"),
                DotName.createSimple("org.aesh.command.converter.Converter"),
                DotName.createSimple("org.aesh.command.validator.OptionValidator"),
                DotName.createSimple("org.aesh.command.validator.CommandValidator"),
                DotName.createSimple("org.aesh.command.activator.CommandActivator"),
                DotName.createSimple("org.aesh.command.activator.OptionActivator"),
                DotName.createSimple("org.aesh.command.result.ResultHandler"),
                DotName.createSimple("org.aesh.command.renderer.OptionRenderer")));
        // Also keep user implementations of the Quarkus CliSettings customizer
        unremovableBean.produce(UnremovableBeanBuildItem.beanTypes(
                DotName.createSimple(io.quarkus.aesh.runtime.CliSettings.class.getName())));
    }

    /**
     * Resolves the execution mode based on configuration and discovered commands.
     * The mode is always resolved to either {@link AeshMode#console} or {@link AeshMode#runtime}.
     * <p>
     * When remote transports (SSH, WebSocket) are present and mode is {@code auto},
     * console mode is forced because remote terminals require interactive console infrastructure.
     */
    @BuildStep
    AeshModeBuildItem resolveMode(AeshBuildTimeConfig config,
            CombinedIndexBuildItem combinedIndex,
            List<AeshCommandBuildItem> commands,
            List<AeshRemoteTransportBuildItem> remoteTransports) {
        AeshMode resolvedMode = resolveExecutionMode(config, commands, remoteTransports);
        boolean hasUserDefinedMain = !combinedIndex.getIndex().getAnnotations(QUARKUS_MAIN).isEmpty();
        return new AeshModeBuildItem(resolvedMode, hasUserDefinedMain);
    }

    // Top command detection is now done in recordAeshMetadata and passed to the recorder.

    /**
     * Registers the appropriate runner beans and QuarkusApplication class
     * based on the resolved mode.
     * <p>
     * When remote transports are present, the local console (stdin/stdout) is skipped
     * by default. The {@code quarkus.aesh.start-console} config can override this behavior.
     */
    @BuildStep
    void registerRunner(AeshModeBuildItem mode,
            AeshBuildTimeConfig config,
            List<AeshRemoteTransportBuildItem> remoteTransports,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<QuarkusApplicationClassBuildItem> quarkusApplicationClass) {
        if (mode.hasUserDefinedMain()) {
            return;
        }
        if (mode.getResolvedMode() == AeshMode.console) {
            boolean startConsole = config.startConsole().orElse(remoteTransports.isEmpty());

            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(DefaultCliCommandRegistryFactory.class));
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(AeshRemoteConnectionHandler.class));

            if (startConsole) {
                additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(CliRunner.class));
                quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(CliRunner.class));
            }
        } else {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(AeshRunner.class));
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(DefaultAeshRuntimeRunnerFactory.class));
            quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(AeshRunner.class));
        }
    }

    /**
     * Records build-time command metadata and creates a synthetic {@link AeshContext} bean
     * that is available for injection at runtime.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void recordAeshMetadata(AeshRecorder recorder,
            AeshModeBuildItem mode,
            List<AeshCommandBuildItem> commands,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        List<AeshCommandMetadata> metadata = new ArrayList<>();
        for (AeshCommandBuildItem cmd : commands) {
            AeshCommandMetadata m = new AeshCommandMetadata();
            m.setClassName(cmd.getClassName());
            m.setCommandName(cmd.getCommandName());
            m.setDescription(cmd.getDescription());
            m.setGroupCommand(cmd.isGroupCommand());
            m.setSubCommandClassNames(cmd.getSubCommandClassNames());
            metadata.add(m);
        }

        // Detect top command for runtime mode
        String topCommandClassName = null;
        if (mode.getResolvedMode() == AeshMode.runtime) {
            topCommandClassName = detectTopCommand(commands);
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(AeshContext.class)
                .setRuntimeInit()
                .supplier(recorder.createContext(metadata, mode.getResolvedMode().name(), topCommandClassName))
                .done());
    }

    // --- Private helper methods ---

    private AeshMode resolveExecutionMode(AeshBuildTimeConfig config, List<AeshCommandBuildItem> commands,
            List<AeshRemoteTransportBuildItem> remoteTransports) {
        AeshMode mode = config.mode();

        if (mode == AeshMode.console) {
            return AeshMode.console;
        }
        if (mode == AeshMode.runtime) {
            return AeshMode.runtime;
        }

        // Auto mode: if remote transports are present, force console mode
        // because remote terminals require interactive console infrastructure
        if (!remoteTransports.isEmpty()) {
            return AeshMode.console;
        }

        // Check if a single group command encompasses all other commands (recursively)
        // If so, it's a runtime application (git-style CLI)
        if (detectTopCommand(commands) != null) {
            return AeshMode.runtime;
        }

        // Single command → runtime; multiple independent commands → console
        return commands.size() > 1 ? AeshMode.console : AeshMode.runtime;
    }

    /**
     * Detects the top command class name from the discovered commands.
     * Returns the class name of the top command, or {@code null} if none can be determined.
     */
    private String detectTopCommand(List<AeshCommandBuildItem> commands) {
        // Build a map for quick lookup
        Map<String, AeshCommandBuildItem> byClass = new java.util.HashMap<>();
        for (AeshCommandBuildItem cmd : commands) {
            byClass.put(cmd.getClassName(), cmd);
        }

        Set<String> allClassNames = byClass.keySet();

        // 1. If there's a group command whose sub-commands (recursively) include
        //    all other commands, it's the top command (git-style CLI)
        for (AeshCommandBuildItem cmd : commands) {
            if (cmd.isGroupCommand()) {
                Set<String> covered = new HashSet<>();
                collectSubCommands(cmd, byClass, covered);
                covered.add(cmd.getClassName());
                if (covered.containsAll(allClassNames)) {
                    return cmd.getClassName();
                }
            }
        }

        // 2. If there's exactly one command, it's the top command
        if (commands.size() == 1) {
            return commands.get(0).getClassName();
        }

        return null;
    }

    /**
     * Recursively collects all sub-command class names from a group command.
     */
    private void collectSubCommands(AeshCommandBuildItem cmd, Map<String, AeshCommandBuildItem> byClass,
            Set<String> collected) {
        for (String subClassName : cmd.getSubCommandClassNames()) {
            if (collected.add(subClassName)) {
                AeshCommandBuildItem subCmd = byClass.get(subClassName);
                if (subCmd != null && subCmd.isGroupCommand()) {
                    collectSubCommands(subCmd, byClass, collected);
                }
            }
        }
    }

    private List<String> extractGroupSubCommands(AnnotationInstance groupAnnotation) {
        AnnotationValue groupCommandsValue = groupAnnotation.value("groupCommands");
        if (groupCommandsValue == null) {
            return List.of();
        }
        List<String> subCommands = new ArrayList<>();
        for (Type type : groupCommandsValue.asClassArray()) {
            subCommands.add(type.name().toString());
        }
        return subCommands;
    }

    private String getAnnotationStringValue(AnnotationInstance annotation, String name, String defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asString() : defaultValue;
    }
}
