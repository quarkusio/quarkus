package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;

import io.quarkus.builder.Version;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.ClassOutputBuildItem;
import io.quarkus.deployment.builditem.ExecutionChainBuildItem;
import io.quarkus.deployment.builditem.ExecutionHandlerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.execution.ConfigurationHandler;
import io.quarkus.runtime.execution.ExecutionChain;
import io.quarkus.runtime.execution.ExecutionContext;
import io.quarkus.runtime.execution.ExecutionHandler;
import io.quarkus.runtime.execution.SupplierExecutionHandler;

/**
 * A build step which assembles all of the execution handlers into a serialized chain.
 */
public final class ExecutionHandlersBuildStep {

    private static final String INIT_CLASS_NAME = "io.quarkus.runtime.generated.Init";
    private static final String VERSION_AND_FEATURES_CLASS_NAME = "io.quarkus.runtime.generated.VersionAndFeaturesHandler";
    private static final String SYSTEM_PROPS_CLASS_NAME = "io.quarkus.runtime.generated.SystemProperties";
    private static final MethodDescriptor SET_PROPERTY = MethodDescriptor.ofMethod(System.class, "setProperty", String.class,
            String.class, String.class);
    private static final FieldDescriptor EMPTY = FieldDescriptor.of(ExecutionContext.class, "EMPTY", ExecutionContext.class);
    private static final MethodDescriptor ADD_VALUES_TO = MethodDescriptor.ofMethod(StartupContext.class, "addValuesTo",
            ExecutionContext.class, ExecutionContext.class);
    private static final MethodDescriptor WITH_CLOSEABLE = MethodDescriptor.ofMethod(ExecutionContext.class, "withCloseable",
            ExecutionContext.class, AutoCloseable.class);
    private static final MethodDescriptor WITH_VALUES = MethodDescriptor.ofMethod(ExecutionContext.class, "withValues",
            ExecutionContext.class, String.class, Object.class, String.class, Object.class);
    static final MethodDescriptor PROCEED = ofMethod(ExecutionChain.class, "proceed", int.class, ExecutionContext.class);

    @BuildStep
    @Produce(QuarkusApplicationBuildItem.class)
    public void generateStaticInitRecorders(
            ClassOutputBuildItem outputItem,
            List<StaticBytecodeRecorderBuildItem> staticInitTasks,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders) {

        try (ClassCreator cc = ClassCreator.builder().className(INIT_CLASS_NAME)
                .classOutput(ClassOutput.gizmoAdaptor(outputItem.getClassOutput(), true))
                .setFinal(true).superClass(Object.class).build()) {

            try (MethodCreator mv = cc.getMethodCreator("getInitialContext", ExecutionContext.class)) {
                mv.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

                // set sys props
                mv.invokeStaticMethod(ofMethod(SYSTEM_PROPS_CLASS_NAME, "setProperties", void.class));

                // call generated steps
                ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
                try (TryBlock tryBlock = mv.tryBlock()) {
                    for (StaticBytecodeRecorderBuildItem recItem : staticInitTasks) {
                        final BytecodeRecorderImpl recorder = recItem.getBytecodeRecorder();
                        if (!recorder.isEmpty()) {
                            // Register substitutions in all recorders
                            for (ObjectSubstitutionBuildItem sub : substitutions) {
                                ObjectSubstitutionBuildItem.Holder holder1 = sub.holder;
                                recorder.registerSubstitution(holder1.from, holder1.to, holder1.substitution);
                            }
                            for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                                recorder.registerObjectLoader(item.getObjectLoader());
                            }
                            recorder.writeBytecode(outputItem.getClassOutput());

                            tryBlock.invokeStaticMethod(
                                    ofMethod(recorder.getClassName(), "initialize", void.class, StartupContext.class),
                                    startupContext);
                        }
                    }
                    final ResultHandle empty = tryBlock.readStaticField(EMPTY);
                    final ResultHandle ctx1 = tryBlock.invokeVirtualMethod(ADD_VALUES_TO, startupContext, empty);
                    final ResultHandle ctx2 = tryBlock.invokeVirtualMethod(WITH_CLOSEABLE, ctx1, startupContext);
                    tryBlock.returnValue(ctx2);
                    try (CatchBlockCreator cb = tryBlock.addCatch(Throwable.class)) {
                        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
                        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());
                    }
                }
            }
        }
    }

    @BuildStep
    @Produce(QuarkusApplicationBuildItem.class)
    public ExecutionHandlerBuildItem versionAndFeatures(
            List<FeatureBuildItem> featuresItems,
            Consumer<ReflectiveClassBuildItem> reflectiveClasses,
            ClassOutputBuildItem outputItem) {
        try (ClassCreator cc = ClassCreator.builder().className(VERSION_AND_FEATURES_CLASS_NAME)
                .classOutput(ClassOutput.gizmoAdaptor(outputItem.getClassOutput(), true))
                .setFinal(true).superClass(Object.class).interfaces(ExecutionHandler.class).build()) {
            try (MethodCreator mv = cc.getMethodCreator("run", int.class, ExecutionChain.class, ExecutionContext.class)) {
                ResultHandle features = mv.load(featuresItems.stream()
                        .map(FeatureBuildItem::getInfo)
                        .sorted()
                        .collect(Collectors.joining(", ")));
                ResultHandle version = mv.load(Version.getVersion());
                final ResultHandle newCtx = mv.invokeVirtualMethod(WITH_VALUES, mv.getMethodParam(1),
                        mv.readStaticField(FieldDescriptor.of(ExecutionChain.class, "QUARKUS_VERSION", String.class)),
                        version,
                        mv.readStaticField(FieldDescriptor.of(ExecutionChain.class, "QUARKUS_FEATURES", String.class)),
                        features);
                final ResultHandle result = mv.invokeVirtualMethod(PROCEED, mv.getMethodParam(0), newCtx);
                mv.returnValue(result);
            }
            reflectiveClasses.accept(new ReflectiveClassBuildItem(true, false, false, VERSION_AND_FEATURES_CLASS_NAME));
            return new ExecutionHandlerBuildItem(new SupplierExecutionHandler(VERSION_AND_FEATURES_CLASS_NAME));
        }
    }

    @BuildStep
    @Produce(QuarkusApplicationBuildItem.class)
    public void generateMainRecorders(
            ClassOutputBuildItem outputItem,
            List<MainBytecodeRecorderBuildItem> mainTasks,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            Consumer<ReflectiveClassBuildItem> reflectiveClasses,
            Consumer<ExecutionHandlerBuildItem> executionHandlers) {

        for (MainBytecodeRecorderBuildItem recItem : mainTasks) {
            final BytecodeRecorderImpl recorder = recItem.getBytecodeRecorder();
            if (!recorder.isEmpty()) {
                // Register substitutions in all recorders
                for (ObjectSubstitutionBuildItem sub : substitutions) {
                    ObjectSubstitutionBuildItem.Holder holder1 = sub.holder;
                    recorder.registerSubstitution(holder1.from, holder1.to, holder1.substitution);
                }
                for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                    recorder.registerObjectLoader(item.getObjectLoader());
                }
                recorder.writeBytecode(outputItem.getClassOutput());
                executionHandlers.accept(new ExecutionHandlerBuildItem(new SupplierExecutionHandler(recorder.getClassName())));
                reflectiveClasses.accept(new ReflectiveClassBuildItem(true, false, false, recorder.getClassName()));
            }
        }
    }

    @BuildStep
    @Produce(QuarkusApplicationBuildItem.class)
    public GeneratedResourceBuildItem serializeExecutionChain(ExecutionChainBuildItem chainBuildItem) throws IOException {
        ExecutionChain chain = chainBuildItem.getChain();

        // Serialize the chain
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(chain);
            }
            return new GeneratedResourceBuildItem("META-INF/quarkus.init", os.toByteArray());
        }
    }

    @BuildStep
    @Produce(QuarkusApplicationBuildItem.class)
    public ExecutionChainBuildItem assembleHandlers(
            List<ExecutionHandlerBuildItem> handlerItems,
            List<SystemPropertyBuildItem> propertyItems,
            Consumer<ReflectiveClassBuildItem> reflectiveClasses,
            ClassOutputBuildItem outputItem) {
        ExecutionChain chain = null;
        final ListIterator<ExecutionHandlerBuildItem> itr = handlerItems.listIterator(handlerItems.size());
        while (itr.hasPrevious()) {
            final ExecutionHandlerBuildItem item = itr.previous();
            final ExecutionHandler handler = item.getHandler();
            chain = new ExecutionChain(chain, handler);
        }
        Map<String, String> properties = new TreeMap<>();
        for (SystemPropertyBuildItem propertyItem : propertyItems) {
            if (properties.putIfAbsent(propertyItem.getKey(), propertyItem.getValue()) != null) {
                throw new IllegalArgumentException("Multiple values for property \"" + propertyItem.getKey() + "\" given");
            }
        }
        // TODO: temporary
        chain = new ExecutionChain(chain, ConfigurationHandler.getInstance());

        // System properties
        try (ClassCreator cc = ClassCreator.builder().className(SYSTEM_PROPS_CLASS_NAME)
                .classOutput(ClassOutput.gizmoAdaptor(outputItem.getClassOutput(), true))
                .setFinal(true).superClass(Object.class).interfaces(ExecutionHandler.class).build()) {

            try (MethodCreator run = cc.getMethodCreator("run", int.class, ExecutionChain.class, ExecutionContext.class)) {
                run.invokeStaticMethod(MethodDescriptor.ofMethod(SYSTEM_PROPS_CLASS_NAME, "setProperties", void.class));
                run.returnValue(run.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(ExecutionChain.class, "proceed", int.class, ExecutionContext.class),
                        run.getMethodParam(0), run.getMethodParam(1)));
            }

            try (MethodCreator sp = cc.getMethodCreator("setProperties", void.class)) {
                sp.setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC);
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    sp.invokeStaticMethod(SET_PROPERTY, sp.load(entry.getKey()), sp.load(entry.getValue()));
                }
                sp.returnValue(null);
            }
        }

        chain = new ExecutionChain(chain, new SupplierExecutionHandler(SYSTEM_PROPS_CLASS_NAME));
        reflectiveClasses.accept(new ReflectiveClassBuildItem(true, false, false, SYSTEM_PROPS_CLASS_NAME));
        return new ExecutionChainBuildItem(chain);
    }
}
