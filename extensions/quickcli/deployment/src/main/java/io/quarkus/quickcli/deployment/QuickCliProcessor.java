package io.quarkus.quickcli.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.quickcli.runtime.DefaultQuickCliCommandLineFactory;
import io.quarkus.quickcli.runtime.QuickCliConfigSourceBuilder;
import io.quarkus.quickcli.runtime.QuickCliRunner;
import io.quarkus.quickcli.runtime.annotations.TopCommand;
import io.quarkus.runtime.annotations.QuarkusMain;

class QuickCliProcessor {

    private static final String FEATURE = "quickcli";
    private static final DotName QUICKCLI_COMMAND = DotName.createSimple(
            io.quarkus.quickcli.annotations.Command.class.getName());
    private static final DotName TOP_COMMAND = DotName.createSimple(TopCommand.class.getName());
    private static final DotName QUARKUS_MAIN = DotName.createSimple(QuarkusMain.class.getName());
    private static final DotName QUICKCLI_OPTION = DotName.createSimple(
            io.quarkus.quickcli.annotations.Option.class.getName());
    private static final DotName QUICKCLI_PARAMETERS = DotName.createSimple(
            io.quarkus.quickcli.annotations.Parameters.class.getName());
    private static final DotName QUICKCLI_MIXIN = DotName.createSimple(
            io.quarkus.quickcli.annotations.Mixin.class.getName());
    private static final DotName QUICKCLI_PARENT_COMMAND = DotName.createSimple(
            io.quarkus.quickcli.annotations.ParentCommand.class.getName());
    private static final DotName QUICKCLI_SPEC = DotName.createSimple(
            io.quarkus.quickcli.annotations.Spec.class.getName());
    private static final DotName QUICKCLI_UNMATCHED = DotName.createSimple(
            io.quarkus.quickcli.annotations.Unmatched.class.getName());
    private static final DotName QUICKCLI_ARG_GROUP = DotName.createSimple(
            io.quarkus.quickcli.annotations.ArgGroup.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void addScopeToCommands(BuildProducer<AutoAddScopeBuildItem> autoAddScope) {
        // Add @Dependent to all @Command classes so they can participate in CDI
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(QUICKCLI_COMMAND)
                .defaultScope(BuiltinScope.DEPENDENT)
                .unremovable()
                .build());
        // Add @Dependent to any class annotated with @TopCommand
        autoAddScope.produce(AutoAddScopeBuildItem.builder()
                .isAnnotatedWith(TOP_COMMAND)
                .defaultScope(BuiltinScope.DEPENDENT)
                .unremovable()
                .build());
    }

    @BuildStep
    void quickCliRunner(ApplicationIndexBuildItem applicationIndex,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean,
            BuildProducer<QuarkusApplicationClassBuildItem> quarkusApplicationClass,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {

        IndexView index = combinedIndex.getIndex();
        Collection<DotName> topCommands = classesAnnotatedWith(index, TOP_COMMAND);

        if (topCommands.isEmpty()) {
            List<DotName> commands = classesAnnotatedWith(
                    applicationIndex.getIndex(), QUICKCLI_COMMAND);
            if (commands.size() == 1) {
                // If there is exactly one @Command, automatically make it @TopCommand
                DotName singleCommandClassName = commands.get(0);
                annotationsTransformer.produce(new AnnotationsTransformerBuildItem(
                        AnnotationTransformation.forClasses()
                                .whenClass(singleCommandClassName)
                                .priority(2000)
                                .transform(ctx -> ctx.add(TopCommand.class))));
            }
        }

        // Register all @Command classes as CDI beans so they can use @Inject
        // Do NOT set a default scope here — AutoAddScopeBuildItem already handles it,
        // and setting @Dependent here conflicts with QuarkusApplication's @ApplicationScoped.
        List<DotName> allCommands = classesAnnotatedWith(index, QUICKCLI_COMMAND);
        for (DotName commandClass : allCommands) {
            additionalBean.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(commandClass.toString())
                    .setUnremovable()
                    .build());
        }

        if (index.getAnnotations(QUARKUS_MAIN).isEmpty()) {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(QuickCliRunner.class));
            additionalBean.produce(
                    AdditionalBeanBuildItem.unremovableOf(DefaultQuickCliCommandLineFactory.class));
            quarkusApplicationClass.produce(
                    new QuarkusApplicationClassBuildItem(QuickCliRunner.class));
        }

        // Make VersionProvider implementations unremovable
        unremovableBean.produce(UnremovableBeanBuildItem.beanTypes(
                io.quarkus.quickcli.VersionProvider.class));
    }

    @BuildStep
    void registerConfigSource(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(QuickCliConfigSourceBuilder.class));
    }

    @BuildStep
    void generateCommandModels(CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        new CommandModelGenerator(combinedIndex.getIndex()).generateAll(generatedClass);
    }

    @BuildStep
    void transformPrivateFields(CombinedIndexBuildItem combinedIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {

        IndexView index = combinedIndex.getIndex();

        // Collect private annotated fields on @Command classes (and their superclasses)
        // that need to be made package-private. The generated *_QuickCliModel class
        // (same package) uses PUTFIELD to set fields directly.
        Map<String, List<FieldInfo>> classFieldsToTransform = new HashMap<>();

        for (DotName annotation : List.of(QUICKCLI_OPTION, QUICKCLI_PARAMETERS,
                QUICKCLI_MIXIN, QUICKCLI_PARENT_COMMAND, QUICKCLI_SPEC,
                QUICKCLI_UNMATCHED, QUICKCLI_ARG_GROUP)) {
            for (var instance : index.getAnnotations(annotation)) {
                if (instance.target().kind() != AnnotationTarget.Kind.FIELD) {
                    continue;
                }
                FieldInfo field = instance.target().asField();
                if ((field.flags() & Opcodes.ACC_PRIVATE) == 0) {
                    continue;
                }
                String className = field.declaringClass().name().toString();
                classFieldsToTransform
                        .computeIfAbsent(className, k -> new java.util.ArrayList<>())
                        .add(field);
            }
        }

        // Also collect non-public annotated fields and methods on mixin/arggroup types
        // that are used from commands in a different package. The model class navigates through
        // the mixin field chain (e.g., cmd.output.showErrors), and needs the mixin type's
        // members to be public for cross-package access. This is equivalent to picocli's reflection.
        Map<String, List<MethodInfo>> classMethodsToTransform = new HashMap<>();
        collectCrossPackageMixinMembers(index, classFieldsToTransform, classMethodsToTransform);

        // Merge all class names that need transformation
        java.util.Set<String> allClasses = new java.util.HashSet<>();
        allClasses.addAll(classFieldsToTransform.keySet());
        allClasses.addAll(classMethodsToTransform.keySet());

        for (String className : allClasses) {
            List<FieldInfo> fields = classFieldsToTransform.getOrDefault(className, List.of());
            List<MethodInfo> methods = classMethodsToTransform.getOrDefault(className, List.of());

            transformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(className)
                    .setCacheable(true)
                    .setVisitorFunction((name, classVisitor) -> {
                        ClassTransformer transformer = new ClassTransformer(className);
                        for (FieldInfo field : fields) {
                            if ((field.flags() & Opcodes.ACC_PRIVATE) != 0) {
                                // Private → package-private (same as Arc)
                                transformer.modifyField(FieldDescriptor.of(field))
                                        .removeModifiers(Opcodes.ACC_PRIVATE);
                            } else if ((field.flags() & Opcodes.ACC_PUBLIC) == 0) {
                                // Package-private or protected → public (for cross-package mixin access)
                                transformer.modifyField(FieldDescriptor.of(field))
                                        .removeModifiers(Opcodes.ACC_PROTECTED)
                                        .addModifiers(Opcodes.ACC_PUBLIC);
                            }
                        }
                        for (MethodInfo method : methods) {
                            if ((method.flags() & Opcodes.ACC_PUBLIC) == 0) {
                                String[] paramTypes = method.parameterTypes().stream()
                                        .map(t -> t.name().toString())
                                        .toArray(String[]::new);
                                transformer.modifyMethod(
                                        io.quarkus.gizmo.MethodDescriptor.ofMethod(
                                                className, method.name(),
                                                method.returnType().name().toString(),
                                                paramTypes))
                                        .removeModifiers(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)
                                        .addModifiers(Opcodes.ACC_PUBLIC);
                            }
                        }
                        return transformer.applyTo(classVisitor);
                    })
                    .build());
        }
    }

    /**
     * Finds non-public annotated fields and methods on mixin/arggroup types that are referenced
     * from @Command classes in a different package. These members need to be made public
     * so the generated model class can access them through the mixin field chain.
     */
    private void collectCrossPackageMixinMembers(IndexView index,
            Map<String, List<FieldInfo>> classFieldsToTransform,
            Map<String, List<MethodInfo>> classMethodsToTransform) {

        // Find all @Command classes and their packages
        java.util.Set<String> commandPackages = new java.util.HashSet<>();
        for (var ann : index.getAnnotations(QUICKCLI_COMMAND)) {
            if (ann.target().kind() == AnnotationTarget.Kind.CLASS) {
                String className = ann.target().asClass().name().toString();
                commandPackages.add(packageOf(className));
            }
        }

        // Find mixin/arggroup types referenced from @Command classes
        java.util.Set<String> mixinArgGroupTypes = new java.util.HashSet<>();
        for (DotName annotation : List.of(QUICKCLI_MIXIN, QUICKCLI_ARG_GROUP)) {
            for (var instance : index.getAnnotations(annotation)) {
                if (instance.target().kind() != AnnotationTarget.Kind.FIELD) {
                    continue;
                }
                FieldInfo field = instance.target().asField();
                String mixinTypeName = field.type().name().toString();
                String mixinPkg = packageOf(mixinTypeName);
                for (String cmdPkg : commandPackages) {
                    if (!cmdPkg.equals(mixinPkg)) {
                        mixinArgGroupTypes.add(mixinTypeName);
                        break;
                    }
                }
            }
        }

        // For each cross-package mixin/arggroup type, collect non-public annotated members
        for (String typeName : mixinArgGroupTypes) {
            org.jboss.jandex.ClassInfo classInfo = index.getClassByName(typeName);
            if (classInfo == null) {
                continue;
            }
            for (FieldInfo field : classInfo.fields()) {
                if ((field.flags() & Opcodes.ACC_PUBLIC) != 0) {
                    continue;
                }
                boolean hasAnnotation = field.annotation(QUICKCLI_OPTION) != null
                        || field.annotation(QUICKCLI_PARAMETERS) != null
                        || field.annotation(QUICKCLI_MIXIN) != null
                        || field.annotation(QUICKCLI_SPEC) != null
                        || field.annotation(QUICKCLI_PARENT_COMMAND) != null
                        || field.annotation(QUICKCLI_UNMATCHED) != null
                        || field.annotation(QUICKCLI_ARG_GROUP) != null;
                if (hasAnnotation) {
                    classFieldsToTransform
                            .computeIfAbsent(typeName, k -> new java.util.ArrayList<>())
                            .add(field);
                }
            }
            for (MethodInfo method : classInfo.methods()) {
                if ((method.flags() & Opcodes.ACC_PUBLIC) != 0) {
                    continue;
                }
                if (method.annotation(QUICKCLI_OPTION) != null) {
                    classMethodsToTransform
                            .computeIfAbsent(typeName, k -> new java.util.ArrayList<>())
                            .add(method);
                }
            }
        }
    }

    private static String packageOf(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    private List<DotName> classesAnnotatedWith(IndexView indexView, DotName annotationName) {
        return indexView.getAnnotations(annotationName)
                .stream()
                .filter(ann -> ann.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(ann -> ann.target().asClass().name())
                .collect(Collectors.toList());
    }
}
