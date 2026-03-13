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
import io.quarkus.aesh.runtime.AeshProducer;
import io.quarkus.aesh.runtime.AeshRecorder;
import io.quarkus.aesh.runtime.AeshRemoteConnectionHandler;
import io.quarkus.aesh.runtime.AeshRunner;
import io.quarkus.aesh.runtime.CliRunner;
import io.quarkus.aesh.runtime.DefaultAeshRuntimeRunnerFactory;
import io.quarkus.aesh.runtime.DefaultCliCommandRegistryFactory;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.runtime.annotations.QuarkusMain;

class AeshProcessor {

    private static final DotName AESH_COMMAND = DotName.createSimple("org.aesh.command.Command");
    private static final DotName COMMAND_DEFINITION = DotName.createSimple("org.aesh.command.CommandDefinition");
    private static final DotName GROUP_COMMAND_DEFINITION = DotName.createSimple("org.aesh.command.GroupCommandDefinition");
    private static final DotName TOP_COMMAND = DotName.createSimple(TopCommand.class.getName());
    private static final DotName CLI_COMMAND = DotName.createSimple(CliCommand.class.getName());
    private static final DotName QUARKUS_MAIN = DotName.createSimple(QuarkusMain.class.getName());
    private static final DotName PARENT_COMMAND = DotName.createSimple("org.aesh.command.option.ParentCommand");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AESH);
    }

    @BuildStep
    ModuleEnableNativeAccessBuildItem allowNativeLibraryLoad() {
        // Need to generate an --enable-native-access because of org.fusesource.jansi.internal.JansiLoader
        return new ModuleEnableNativeAccessBuildItem("org.fusesource.jansi");
    }

    /**
     * Discovers all command classes from both the application and combined indices.
     * Produces an {@link AeshCommandBuildItem} for each discovered command, capturing
     * the annotation metadata needed by subsequent build steps.
     */
    @BuildStep
    void discoverCommands(ApplicationIndexBuildItem applicationIndex,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AeshCommandBuildItem> commands) {
        IndexView appIndex = applicationIndex.getIndex();
        IndexView combIndex = combinedIndex.getIndex();

        // Track discovered class names to avoid duplicates
        Set<DotName> discovered = new HashSet<>();

        // Scan application index (user code) -- all annotations
        discoverFromIndex(appIndex, discovered, commands, false);

        // Scan combined index (user + libraries) -- filter out org.aesh.* internal classes
        discoverFromIndex(combIndex, discovered, commands, true);
    }

    private void discoverFromIndex(IndexView index, Set<DotName> discovered,
            BuildProducer<AeshCommandBuildItem> commands, boolean filterAeshInternal) {
        // Process @CommandDefinition classes
        for (AnnotationInstance ann : index.getAnnotations(COMMAND_DEFINITION)) {
            if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo classInfo = ann.target().asClass();
            DotName className = classInfo.name();
            if (filterAeshInternal && className.toString().startsWith("org.aesh.")) {
                continue;
            }
            if (!discovered.add(className)) {
                continue;
            }

            String commandName = getAnnotationStringValue(ann, "name", "");
            String description = getAnnotationStringValue(ann, "description", "");
            boolean hasTopCommand = classInfo.hasAnnotation(TOP_COMMAND);
            boolean hasCliCommand = classInfo.hasAnnotation(CLI_COMMAND);

            commands.produce(new AeshCommandBuildItem(
                    className.toString(), commandName, description,
                    false, List.of(), hasTopCommand, hasCliCommand));
        }

        // Process @GroupCommandDefinition classes
        for (AnnotationInstance ann : index.getAnnotations(GROUP_COMMAND_DEFINITION)) {
            if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo classInfo = ann.target().asClass();
            DotName className = classInfo.name();
            if (filterAeshInternal && className.toString().startsWith("org.aesh.")) {
                continue;
            }
            if (!discovered.add(className)) {
                continue;
            }

            String commandName = getAnnotationStringValue(ann, "name", "");
            String description = getAnnotationStringValue(ann, "description", "");
            boolean hasTopCommand = classInfo.hasAnnotation(TOP_COMMAND);
            boolean hasCliCommand = classInfo.hasAnnotation(CLI_COMMAND);
            List<String> subCommandClassNames = extractGroupSubCommands(ann);

            commands.produce(new AeshCommandBuildItem(
                    className.toString(), commandName, description,
                    true, subCommandClassNames, hasTopCommand, hasCliCommand));
        }

        // Process classes that have @TopCommand or @CliCommand but no @CommandDefinition/@GroupCommandDefinition
        // (these would be rare but handle them for completeness)
        for (DotName qualifierAnnotation : List.of(TOP_COMMAND, CLI_COMMAND)) {
            for (AnnotationInstance ann : index.getAnnotations(qualifierAnnotation)) {
                if (ann.target().kind() != AnnotationTarget.Kind.CLASS) {
                    continue;
                }
                DotName className = ann.target().asClass().name();
                if (!discovered.add(className)) {
                    continue;
                }
                // These don't have @CommandDefinition so we have limited metadata
                commands.produce(new AeshCommandBuildItem(
                        className.toString(), "", "",
                        false, List.of(),
                        qualifierAnnotation.equals(TOP_COMMAND),
                        qualifierAnnotation.equals(CLI_COMMAND)));
            }
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

        // 3. Multiple @TopCommand annotations
        List<AeshCommandBuildItem> topCommands = commands.stream()
                .filter(AeshCommandBuildItem::isTopCommand)
                .collect(Collectors.toList());
        if (topCommands.size() > 1) {
            String classes = topCommands.stream()
                    .map(AeshCommandBuildItem::getClassName)
                    .collect(Collectors.joining(", "));
            errors.add(new IllegalStateException(
                    "Multiple @TopCommand annotations found, but only one command can serve as the top-level entry point: "
                            + classes));
        }

        // 4. Conflicting @TopCommand + @CliCommand on the same class
        for (AeshCommandBuildItem cmd : commands) {
            if (cmd.isTopCommand() && cmd.isCliCommand()) {
                errors.add(new IllegalStateException(
                        "Command class '" + cmd.getClassName()
                                + "' has both @TopCommand and @CliCommand annotations, "
                                + "which represent mutually exclusive execution modes"));
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
        // Also add @Dependent to any class annotated with @GroupCommandDefinition
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(GROUP_COMMAND_DEFINITION)
                .defaultScope(BuiltinScope.DEPENDENT)
                .priority(20)
                .unremovable()
                .build());
        // Also add @Dependent to any class annotated with @TopCommand
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(TOP_COMMAND)
                .defaultScope(BuiltinScope.DEPENDENT)
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
        // Add @Dependent to any class annotated with @CliCommand (console mode commands)
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(CLI_COMMAND)
                .defaultScope(BuiltinScope.DEPENDENT)
                .unremovable()
                .build());
    }

    @BuildStep
    void registerAeshBeans(BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean) {
        // Always register AeshProducer so users can inject AeshCommandRegistryBuilder
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(AeshProducer.class));

        // Register aesh annotations as bean-defining annotations so that
        // classes annotated with them are automatically discovered as CDI beans
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(COMMAND_DEFINITION,
                BuiltinScope.DEPENDENT.getName()));
        beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(GROUP_COMMAND_DEFINITION,
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

    /**
     * Transforms annotations based on the resolved mode:
     * <ul>
     * <li>Console mode: adds {@code @CliCommand} to {@code @CommandDefinition} classes
     * that are not sub-commands of a group command</li>
     * <li>Runtime mode: adds {@code @TopCommand} to a single {@code @CommandDefinition}
     * if no {@code @TopCommand} exists</li>
     * </ul>
     */
    @BuildStep
    void transformAnnotations(AeshModeBuildItem mode,
            List<AeshCommandBuildItem> commands,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        if (mode.getResolvedMode() == AeshMode.console) {
            transformForConsoleMode(commands, annotationsTransformer);
        } else {
            transformForRuntimeMode(commands, annotationsTransformer);
        }
    }

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

            // Always register the command registry factory and remote connection handler
            // for console mode -- needed by both local and remote terminals
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(DefaultCliCommandRegistryFactory.class));
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(AeshRemoteConnectionHandler.class));

            if (startConsole) {
                // Start local console: register CliRunner as QuarkusApplication
                additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(CliRunner.class));
                quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(CliRunner.class));
            }
            // When startConsole is false, no QuarkusApplication is registered --
            // the app starts as a normal Quarkus server with remote CLI access only
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
            m.setTopCommand(cmd.isTopCommand());
            m.setCliCommand(cmd.isCliCommand());
            metadata.add(m);
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(AeshContext.class)
                .setRuntimeInit()
                .supplier(recorder.createContext(metadata, mode.getResolvedMode().name()))
                .done());
    }

    // --- Private helper methods ---

    private AeshMode resolveExecutionMode(AeshBuildTimeConfig config, List<AeshCommandBuildItem> commands,
            List<AeshRemoteTransportBuildItem> remoteTransports) {
        String mode = config.mode();

        if ("console".equalsIgnoreCase(mode)) {
            return AeshMode.console;
        }
        if ("runtime".equalsIgnoreCase(mode)) {
            return AeshMode.runtime;
        }

        // Auto mode: if remote transports are present, force console mode
        // because remote terminals require interactive console infrastructure
        if (!remoteTransports.isEmpty()) {
            return AeshMode.console;
        }

        // Auto mode: detect based on discovered command annotations
        if (commands.stream().anyMatch(AeshCommandBuildItem::isCliCommand)) {
            return AeshMode.console;
        }
        if (commands.stream().anyMatch(AeshCommandBuildItem::isTopCommand)) {
            return AeshMode.runtime;
        }
        if (commands.stream().anyMatch(AeshCommandBuildItem::isGroupCommand)) {
            return AeshMode.runtime;
        }

        // Multiple independent commands without grouping = console mode
        long commandCount = commands.stream()
                .filter(c -> !c.isGroupCommand())
                .count();
        return commandCount > 1 ? AeshMode.console : AeshMode.runtime;
    }

    private void transformForConsoleMode(List<AeshCommandBuildItem> commands,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        // Collect sub-command class names from group commands
        Set<String> subCommandClasses = commands.stream()
                .filter(AeshCommandBuildItem::isGroupCommand)
                .flatMap(c -> c.getSubCommandClassNames().stream())
                .collect(Collectors.toSet());

        // Add @CliCommand to @CommandDefinition classes that:
        // - are not already @CliCommand
        // - are not group commands themselves
        // - are not sub-commands of a group command
        for (AeshCommandBuildItem cmd : commands) {
            if (!cmd.isCliCommand() && !cmd.isGroupCommand()
                    && !subCommandClasses.contains(cmd.getClassName())) {
                DotName className = DotName.createSimple(cmd.getClassName());
                annotationsTransformer.produce(new AnnotationsTransformerBuildItem(
                        AnnotationsTransformer.appliedToClass()
                                .whenClass(c -> c.name().equals(className))
                                .priority(2000)
                                .thenTransform(t -> t.add(CliCommand.class))));
            }
        }
    }

    private void transformForRuntimeMode(List<AeshCommandBuildItem> commands,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        boolean hasTopCommand = commands.stream().anyMatch(AeshCommandBuildItem::isTopCommand);
        if (hasTopCommand) {
            return;
        }

        // If there is exactly one non-group @CommandDefinition, promote it to @TopCommand
        List<AeshCommandBuildItem> regularCommands = commands.stream()
                .filter(c -> !c.isGroupCommand())
                .collect(Collectors.toList());
        if (regularCommands.size() == 1) {
            DotName className = DotName.createSimple(regularCommands.get(0).getClassName());
            annotationsTransformer.produce(new AnnotationsTransformerBuildItem(
                    AnnotationsTransformer.appliedToClass()
                            .whenClass(c -> c.name().equals(className))
                            .priority(2000)
                            .thenTransform(t -> t.add(TopCommand.class))));
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
