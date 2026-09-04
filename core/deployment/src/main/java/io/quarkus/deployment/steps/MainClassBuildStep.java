package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.KotlinUtil.isKotlinClass;
import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.bootstrap.naming.DisabledInitialContextManager;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.Version;
import io.quarkus.core.deployment.action.impl.Dependency;
import io.quarkus.core.deployment.action.impl.LambdaTransliterator;
import io.quarkus.core.deployment.action.impl.ServiceValueRetentionBuildItem;
import io.quarkus.core.deployment.action.impl.TransliteratedAction;
import io.quarkus.core.impl.NodeShutdownContext;
import io.quarkus.core.impl.ServiceGraph;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AllowJNDIBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderConstantDefinitionBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedRuntimeSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.PreInitBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.deployment.builditem.RecordableConstructorBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.ValueRegistryRuntimeInfoProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.naming.NamingConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.ExecutionModeManager;
import io.quarkus.runtime.JVMUnsafeWarningsControl;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.NativeImageRuntimePropertiesRecorder;
import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;
import io.quarkus.runtime.ValueRegistryImpl.ConfigRuntimeSource;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.util.StepTiming;
import io.quarkus.value.registry.RuntimeInfoProvider;
import io.quarkus.value.registry.RuntimeInfoProvider.RuntimeSource;
import io.quarkus.value.registry.ValueRegistry;

public class MainClassBuildStep {

    static final String MAIN_CLASS = "io.quarkus.runner.GeneratedMain";
    static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";
    static final String LOG = "LOG";
    static final String JAVA_LIBRARY_PATH = "java.library.path";
    // This is declared as a constant so that it can be grepped for in the native-image binary using `strings`, e.g.:
    // strings ./target/quarkus-runner | grep "__quarkus_analytics__quarkus.version="
    public static final String QUARKUS_ANALYTICS_QUARKUS_VERSION = "__QUARKUS_ANALYTICS_QUARKUS_VERSION";

    public static final String GENERATE_APP_CDS_SYSTEM_PROPERTY = "quarkus.appcds.generate";

    private static final FieldDescriptor STARTUP_CONTEXT_FIELD = FieldDescriptor.of(Application.APP_CLASS_NAME, STARTUP_CONTEXT,
            StartupContext.class);

    public static final MethodDescriptor PRINT_STEP_TIME_METHOD = ofMethod(StepTiming.class.getName(), "printStepTime",
            void.class, StartupContext.class);
    public static final MethodDescriptor CONFIGURE_STEP_TIME_ENABLED = ofMethod(StepTiming.class.getName(), "configureEnabled",
            void.class);
    public static final MethodDescriptor RUNTIME_EXECUTION_STATIC_INIT = ofMethod(ExecutionModeManager.class.getName(),
            "staticInit", void.class);
    public static final MethodDescriptor RUNTIME_EXECUTION_RUNTIME_INIT = ofMethod(ExecutionModeManager.class.getName(),
            "runtimeInit", void.class);
    public static final MethodDescriptor RUNTIME_EXECUTION_RUNNING = ofMethod(ExecutionModeManager.class.getName(),
            "running", void.class);
    public static final MethodDescriptor RUNTIME_EXECUTION_UNSET = ofMethod(ExecutionModeManager.class.getName(),
            "unset", void.class);
    public static final MethodDescriptor CONFIGURE_STEP_TIME_START = ofMethod(StepTiming.class.getName(), "configureStart",
            void.class);
    private static final DotName QUARKUS_APPLICATION = DotName.createSimple(QuarkusApplication.class.getName());
    private static final DotName OBJECT = DotName.createSimple(Object.class.getName());
    private static final Type STRING_ARRAY = Type.create(DotName.createSimple(String[].class.getName()), Type.Kind.ARRAY);

    @BuildStep
    void build(
            BuildContext buildContext,
            List<StaticBytecodeRecorderBuildItem> staticInitTasks,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<ValueRegistryRuntimeInfoProviderBuildItem> runtimeInfoProviders,
            List<MainBytecodeRecorderBuildItem> mainMethod,
            List<SystemPropertyBuildItem> properties,
            List<GeneratedRuntimeSystemPropertyBuildItem> generatedRuntimeSystemProperties,
            List<JavaLibraryPathAdditionalPathBuildItem> javaLibraryPathAdditionalPaths,
            List<FeatureBuildItem> features,
            BuildProducer<ApplicationClassNameBuildItem> appClassNameProducer,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReloadBuildItem,
            ApplicationInfoBuildItem applicationInfo,
            List<AllowJNDIBuildItem> allowJNDIBuildItems,
            Optional<PreInitBuildItem> preInitBuildItem,
            NamingConfig namingConfig,
            List<ServiceValueRetentionBuildItem> retentionItems) {

        appClassNameProducer.produce(new ApplicationClassNameBuildItem(Application.APP_CLASS_NAME));

        // Consolidate transliterated actions into shared classes
        Map<TransliteratedAction, ConsolidatedRef> consolidatedRefs = consolidateActions(
                staticInitTasks, mainMethod, generatedClass);

        // Compute service graph plans from build items + step dependency graph
        Map<String, Set<String>> stepGraph = buildContext.getStepDependencyGraph();
        Map<String, Set<String>> buildItemProducers = buildContext.getBuildItemProducers();
        ServiceGraphBuilder.GraphPlan staticPlan = ServiceGraphBuilder.buildStaticInit(
                staticInitTasks, stepGraph, buildItemProducers);
        ServiceGraphBuilder.GraphPlan runtimePlan = ServiceGraphBuilder.buildRuntime(
                mainMethod, stepGraph, staticPlan.serviceKeys(), buildItemProducers);

        // Application class
        GeneratedClassGizmoAdaptor gizmoOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        ClassCreator file = new ClassCreator(gizmoOutput, Application.APP_CLASS_NAME, null,
                Application.class.getName());

        // Application class: static init

        // LOG static field
        FieldCreator logField = file.getFieldCreator(LOG, Logger.class).setModifiers(Modifier.STATIC);

        FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT_FIELD);
        scField.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        // static fields for service graphs (used by doStop)
        FieldCreator staticGraphField = file.getFieldCreator("STATIC_GRAPH", SERVICE_GRAPH_CLASS);
        staticGraphField.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        FieldCreator runtimeGraphField = file.getFieldCreator("RUNTIME_GRAPH", SERVICE_GRAPH_CLASS);
        runtimeGraphField.setModifiers(Modifier.PRIVATE | Modifier.STATIC);

        FieldCreator quarkusVersionField = file.getFieldCreator(QUARKUS_ANALYTICS_QUARKUS_VERSION, String.class)
                .setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);

        MethodCreator ctor = file.getMethodCreator("<init>", void.class);
        ctor.invokeSpecialMethod(ofMethod(Application.class, "<init>", void.class, boolean.class),
                ctor.getThis(), ctor.load(launchMode.isAuxiliaryApplication()));
        ctor.returnValue(null);

        MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        if (!namingConfig.enableJndi() && allowJNDIBuildItems.isEmpty()) {
            mv.invokeStaticMethod(ofMethod(DisabledInitialContextManager.class, "register", void.class));
        }
        mv.invokeStaticMethod(ofMethod(JVMUnsafeWarningsControl.class, "disableUnsafeRelatedWarnings", void.class));

        // very first thing is to set system props (for build time)
        // make sure we record the system properties in order for build reproducibility
        for (SystemPropertyBuildItem i : properties.stream().sorted(Comparator.comparing(SystemPropertyBuildItem::getKey))
                .toList()) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
        }

        if (preInitBuildItem.isPresent()) {
            // we need to initialize JBoss Logging before starting any parallel work, it's too central
            ResultHandle tccl = mv.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                    mv.invokeStaticMethod(MethodDescriptor.ofMethod(Thread.class, "currentThread", Thread.class)));
            mv.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Class.class, "forName", Class.class, String.class, boolean.class,
                            ClassLoader.class),
                    mv.load("org.jboss.logging.LoggerProviders"), mv.load(true), tccl);

            // then we can preinitialize
            mv.invokeStaticMethod(PreInitBuildStep.PRE_INIT_RUNNER_EXECUTE_PRE_INIT_TASKS_GIZMO1_METHOD);
        }

        //set the launch mode
        ResultHandle lm = mv
                .readStaticField(FieldDescriptor.of(LaunchMode.class, launchMode.getLaunchMode().name(), LaunchMode.class));
        mv.invokeStaticMethod(ofMethod(LaunchMode.class, "set", void.class, LaunchMode.class),
                lm);

        mv.invokeStaticMethod(CONFIGURE_STEP_TIME_ENABLED);
        mv.invokeStaticMethod(RUNTIME_EXECUTION_STATIC_INIT);

        mv.invokeStaticMethod(ofMethod(Timing.class, "staticInitStarted", void.class, boolean.class),
                mv.load(launchMode.isAuxiliaryApplication()));

        // Create Static Init Config and associate it with the current classloader
        mv.invokeStaticMethod(RunTimeConfigurationGenerator.C_STATIC_INIT_CONFIG);

        // Init the LOG instance
        mv.writeStaticField(logField.getFieldDescriptor(), mv.invokeStaticMethod(
                ofMethod(Logger.class, "getLogger", Logger.class, String.class), mv.load("io.quarkus.application")));

        // Init the __QUARKUS_ANALYTICS_QUARKUS_VERSION field
        mv.writeStaticField(quarkusVersionField.getFieldDescriptor(),
                mv.load("__quarkus_analytics__quarkus.version=" + Version.getVersion()));

        TryBlock tryBlock = mv.tryBlock();
        tryBlock.invokeStaticMethod(CONFIGURE_STEP_TIME_START);

        // create the shared StartupContext (used by both static-init and runtime-init graphs)
        ResultHandle startupContext = tryBlock.newInstance(ofConstructor(StartupContext.class));
        tryBlock.writeStaticField(scField.getFieldDescriptor(), startupContext);

        // build and start the static-init service graph
        ResultHandle staticGraph = emitServiceGraph(tryBlock, staticPlan, startupContext, file, consolidatedRefs,
                gizmoOutput, substitutions, recordableConstructorBuildItems, loaders, constants, "static");
        tryBlock.writeStaticField(staticGraphField.getFieldDescriptor(), staticGraph);

        // discard static-init-only service values; retain keys needed at runtime-init
        Set<String> crossPhaseKeys = computeCrossPhaseKeys(mainMethod, retentionItems, staticPlan.serviceKeys());
        if (!crossPhaseKeys.isEmpty()) {
            generateRetainServiceValues(tryBlock, startupContext, crossPhaseKeys);
        }
        tryBlock.returnValue(null);

        CatchBlockCreator cb = tryBlock.addCatch(Throwable.class);
        cb.invokeStaticMethod(ofMethod(ApplicationStateNotification.class, "notifyStartupFailed", void.class, Throwable.class),
                cb.getCaughtException());
        // on failure, stop the static-init graph if it was started
        ResultHandle failGraph = cb.readStaticField(staticGraphField.getFieldDescriptor());
        cb.ifNotNull(failGraph).trueBranch()
                .invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "stop", void.class), failGraph);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());

        // Application class: start method

        mv = file.getMethodCreator("doStart", void.class, String[].class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);

        startupContext = mv.readStaticField(scField.getFieldDescriptor());

        // Register ValueRegistry with StartupContext, so it can be injected into Recorders
        ResultHandle valueRegistry = mv
                .invokeVirtualMethod(ofMethod(Application.class, "getValueRegistry", ValueRegistry.class), mv.getThis());
        MethodDescriptor putValueInStartupContext = ofMethod(StartupContext.class, "putValue", void.class, String.class,
                Object.class);
        mv.invokeVirtualMethod(putValueInStartupContext, startupContext, mv.load(ValueRegistry.class.getName()), valueRegistry);

        // Make sure we set properties in doStartup as well. This is necessary because setting them in the static-init
        // sets them at build-time, on the host JVM, while SVM has substitutions for System. get/setProperty at
        // run-time which will never see those properties unless we also set them at run-time.
        // make sure we record the system properties in order for build reproducibility
        for (SystemPropertyBuildItem i : properties.stream().sorted(Comparator.comparing(SystemPropertyBuildItem::getKey))
                .toList()) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
        }
        // make sure we record the system properties in order for build reproducibility
        for (GeneratedRuntimeSystemPropertyBuildItem i : generatedRuntimeSystemProperties.stream()
                .sorted(Comparator.comparing(GeneratedRuntimeSystemPropertyBuildItem::getKey)).toList()) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()),
                    mv.invokeVirtualMethod(MethodDescriptor.ofMethod(i.getGeneratorClass(), "get", String.class.getName()),
                            mv.newInstance(MethodDescriptor.ofConstructor(i.getGeneratorClass()))));
        }
        mv.invokeStaticMethod(ofMethod(NativeImageRuntimePropertiesRecorder.class, "doRuntime", void.class));
        mv.invokeStaticMethod(RUNTIME_EXECUTION_RUNTIME_INIT);

        // Set the SSL system properties
        if (!javaLibraryPathAdditionalPaths.isEmpty()) {
            ResultHandle javaLibraryPath = mv.newInstance(ofConstructor(StringBuilder.class, String.class),
                    mv.invokeStaticMethod(ofMethod(System.class, "getProperty", String.class, String.class),
                            mv.load(JAVA_LIBRARY_PATH)));
            for (JavaLibraryPathAdditionalPathBuildItem javaLibraryPathAdditionalPath : javaLibraryPathAdditionalPaths) {
                ResultHandle javaLibraryPathLength = mv.invokeVirtualMethod(ofMethod(StringBuilder.class, "length", int.class),
                        javaLibraryPath);
                mv.ifNonZero(javaLibraryPathLength).trueBranch()
                        .invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class),
                                javaLibraryPath, mv.load(File.pathSeparator));
                mv.invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class),
                        javaLibraryPath,
                        mv.load(javaLibraryPathAdditionalPath.getPath()));
            }
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(JAVA_LIBRARY_PATH),
                    mv.invokeVirtualMethod(ofMethod(StringBuilder.class, "toString", String.class), javaLibraryPath));
        }

        mv.invokeStaticMethod(ofMethod(Timing.class, "mainStarted", void.class));

        //now set the command line arguments
        mv.invokeVirtualMethod(
                ofMethod(StartupContext.class, "setCommandLineArguments", void.class, String[].class),
                startupContext, mv.getMethodParam(0));

        mv.invokeStaticMethod(CONFIGURE_STEP_TIME_ENABLED);

        tryBlock = mv.tryBlock();
        tryBlock.invokeStaticMethod(CONFIGURE_STEP_TIME_START);

        // Create Runtime Config and associate it with the current classloader
        tryBlock.invokeStaticMethod(RunTimeConfigurationGenerator.C_RUN_TIME_CONFIG, valueRegistry);

        // Register RuntimeInfoProviders with ValueRegistry
        for (ValueRegistryRuntimeInfoProviderBuildItem runtimeInfoProviderClass : runtimeInfoProviders) {
            ResultHandle runtimeInfoProvider = tryBlock
                    .newInstance(ofConstructor(runtimeInfoProviderClass.getRuntimeInfoProvider()));
            tryBlock.invokeInterfaceMethod(
                    ofMethod(RuntimeInfoProvider.class, "register", void.class, ValueRegistry.class, RuntimeSource.class),
                    runtimeInfoProvider,
                    valueRegistry,
                    tryBlock.invokeStaticMethod(ofMethod(ConfigRuntimeSource.class, "runtimeSource", RuntimeSource.class)));
        }

        // build and start the runtime service graph (shares the same StartupContext)
        ResultHandle runtimeGraph = emitServiceGraph(tryBlock, runtimePlan, startupContext, file, consolidatedRefs,
                gizmoOutput, substitutions, recordableConstructorBuildItems, loaders, constants, "runtime");
        tryBlock.writeStaticField(runtimeGraphField.getFieldDescriptor(), runtimeGraph);

        // discard runtime-init service values; retain only CDI service-value bean keys
        // (those are self-draining: each bean's creation function calls removeServiceValue)
        Set<String> postStartupKeys = computePostStartupKeys(retentionItems);
        if (postStartupKeys.isEmpty()) {
            tryBlock.invokeVirtualMethod(
                    ofMethod(StartupContext.class, "clearServiceValues", void.class), startupContext);
        } else {
            generateRetainServiceValues(tryBlock, startupContext, postStartupKeys);
        }

        tryBlock.invokeStaticMethod(RUNTIME_EXECUTION_RUNNING);

        // Startup log messages
        List<String> featureNames = new ArrayList<>();
        for (FeatureBuildItem feature : features) {
            if (featureNames.contains(feature.getName())) {
                throw new IllegalStateException(
                        "Multiple extensions registered a feature of the same name: " + feature.getName());
            }
            featureNames.add(feature.getName());
        }
        ResultHandle featuresHandle = tryBlock.load(featureNames.stream().sorted().collect(Collectors.joining(", ")));
        tryBlock.invokeStaticMethod(
                ofMethod(Timing.class, "printStartupTime", void.class, String.class, String.class, String.class, String.class,
                        List.class, boolean.class, boolean.class),
                tryBlock.load(applicationInfo.getName()),
                tryBlock.load(applicationInfo.getVersion()),
                tryBlock.load(Version.getVersion()),
                featuresHandle,
                tryBlock.invokeStaticMethod(ofMethod(ConfigUtils.class, "getProfiles", List.class)),
                tryBlock.load(LaunchMode.DEVELOPMENT.equals(launchMode.getLaunchMode())),
                tryBlock.load(launchMode.isAuxiliaryApplication()));

        tryBlock.invokeStaticMethod(
                ofMethod(QuarkusConsole.class, "start", void.class));

        CatchBlockCreator preventFurtherStepsBlock = tryBlock.addCatch(PreventFurtherStepsException.class);
        // stop both graphs to run shutdown handlers before closing the context
        ResultHandle pfRtGraph = preventFurtherStepsBlock.readStaticField(runtimeGraphField.getFieldDescriptor());
        preventFurtherStepsBlock.ifNotNull(pfRtGraph).trueBranch()
                .invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "stop", void.class), pfRtGraph);
        ResultHandle pfStGraph = preventFurtherStepsBlock.readStaticField(staticGraphField.getFieldDescriptor());
        preventFurtherStepsBlock.ifNotNull(pfStGraph).trueBranch()
                .invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "stop", void.class), pfStGraph);
        preventFurtherStepsBlock.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);

        cb = tryBlock.addCatch(Throwable.class);

        // an exception was thrown before logging was actually setup, we simply dump everything to the console
        // we don't do this for dev mode, as on startup failure dev mode sets up its own logging
        if (launchMode.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            ResultHandle delayedHandler = cb
                    .readStaticField(
                            FieldDescriptor.of(InitialConfigurator.class, "DELAYED_HANDLER", QuarkusDelayedHandler.class));
            ResultHandle isActivated = cb.invokeVirtualMethod(
                    ofMethod(QuarkusDelayedHandler.class, "isActivated", boolean.class),
                    delayedHandler);
            BytecodeCreator isActivatedFalse = cb.ifNonZero(isActivated).falseBranch();
            ResultHandle handlersArray = isActivatedFalse.newArray(Handler.class, 1);
            isActivatedFalse.writeArrayValue(handlersArray, 0,
                    isActivatedFalse.newInstance(ofConstructor(ConsoleHandler.class)));
            isActivatedFalse.invokeVirtualMethod(
                    ofMethod(QuarkusDelayedHandler.class, "setHandlers", Handler[].class, Handler[].class),
                    delayedHandler, handlersArray);
            isActivatedFalse.breakScope();
        }

        // stop both graphs to run shutdown handlers (release Vertx, thread pools, etc.)
        ResultHandle failRtGraph = cb.readStaticField(runtimeGraphField.getFieldDescriptor());
        cb.ifNotNull(failRtGraph).trueBranch()
                .invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "stop", void.class), failRtGraph);
        ResultHandle failStGraph = cb.readStaticField(staticGraphField.getFieldDescriptor());
        cb.ifNotNull(failStGraph).trueBranch()
                .invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "stop", void.class), failStGraph);
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());
        mv.returnValue(null);

        // Application class: stop method

        mv = file.getMethodCreator("doStop", void.class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
        mv.invokeStaticMethod(RUNTIME_EXECUTION_UNSET);
        // stop the runtime graph, then the static-init graph
        ResultHandle rtGraph = mv.readStaticField(runtimeGraphField.getFieldDescriptor());
        mv.ifNotNull(rtGraph).trueBranch()
                .invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "stop", void.class), rtGraph);
        mv.writeStaticField(runtimeGraphField.getFieldDescriptor(), mv.loadNull());
        ResultHandle stGraph = mv.readStaticField(staticGraphField.getFieldDescriptor());
        mv.ifNotNull(stGraph).trueBranch()
                .invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "stop", void.class), stGraph);
        mv.writeStaticField(staticGraphField.getFieldDescriptor(), mv.loadNull());
        // close and release the StartupContext (clears values/serviceValues maps)
        startupContext = mv.readStaticField(scField.getFieldDescriptor());
        mv.ifNotNull(startupContext).trueBranch()
                .invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        mv.writeStaticField(scField.getFieldDescriptor(), mv.loadNull());
        mv.returnValue(null);

        // getName method
        mv = file.getMethodCreator("getName", String.class);
        mv.returnValue(mv.load(applicationInfo.getName()));

        // Finish application class
        file.close();
    }

    @BuildStep
    public MainClassBuildItem mainClassBuildStep(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<BytecodeTransformerBuildItem> transformedClass,
            CombinedIndexBuildItem combinedIndexBuildItem,
            Optional<QuarkusApplicationClassBuildItem> quarkusApplicationClass,
            PackageConfig packageConfig) {
        String mainClassName = MAIN_CLASS;
        Map<String, String> quarkusMainAnnotations = new HashMap<>();
        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> quarkusMains = index
                .getAnnotations(DotName.createSimple(QuarkusMain.class.getName()));
        for (AnnotationInstance i : quarkusMains) {
            AnnotationValue nameValue = i.value("name");
            String name = "";
            if (nameValue != null) {
                name = nameValue.asString();
            }
            ClassInfo classInfo = i.target().asClass();
            if (quarkusMainAnnotations.containsKey(name)) {
                throw new RuntimeException(
                        "More than one @QuarkusMain method found with name '" + name + "': "
                                + classInfo.name() + " and " + quarkusMainAnnotations.get(name));
            }
            quarkusMainAnnotations.put(name, sanitizeMainClassName(classInfo, index));
        }

        MethodInfo mainClassMethod = null;
        if (packageConfig.mainClass().isPresent()) {
            String mainAnnotationClass = quarkusMainAnnotations.get(packageConfig.mainClass().get());
            if (mainAnnotationClass != null) {
                mainClassName = mainAnnotationClass;
            } else {
                mainClassName = packageConfig.mainClass().get();
            }
        } else if (quarkusMainAnnotations.containsKey("")) {
            mainClassName = quarkusMainAnnotations.get("");
        }
        if (mainClassName.equals(MAIN_CLASS)) {
            if (quarkusApplicationClass.isPresent()) {
                //user has not supplied main class, but extension did.
                generateMainForQuarkusApplication(quarkusApplicationClass.get().getClassName(), generatedClass);
            } else {
                //generate a main that just runs the app, the user has not supplied a main class
                ClassCreator file = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClass, true), MAIN_CLASS, null,
                        Object.class.getName());

                MethodCreator mv = file.getMethodCreator("main", void.class, String[].class);
                mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
                mv.invokeStaticMethod(ofMethod(Quarkus.class, "run", void.class, String[].class),
                        mv.getMethodParam(0));
                mv.returnValue(null);

                file.close();
            }
        } else {
            Collection<ClassInfo> impls = index
                    .getAllKnownImplementors(QUARKUS_APPLICATION);
            ClassInfo classByName = index.getClassByName(DotName.createSimple(mainClassName));
            if (classByName != null) {
                mainClassMethod = classByName
                        .method("main", STRING_ARRAY);
            }
            if (mainClassMethod == null) {
                boolean found = false;
                for (ClassInfo i : impls) {
                    if (i.name().toString().equals(mainClassName)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    //this is QuarkusApplication, generate a real main to run it
                    generateMainForQuarkusApplication(mainClassName, generatedClass);
                    mainClassName = MAIN_CLASS;
                } else {
                    ClassInfo classInfo = index.getClassByName(DotName.createSimple(mainClassName));
                    if (classInfo == null) {
                        throw new IllegalArgumentException("The supplied 'main-class' value of '" + mainClassName
                                + "' does not correspond to either a fully qualified class name or a matching 'name' field of one of the '@QuarkusMain' annotations");
                    }
                }
            }
        }

        if (!mainClassName.equals(MAIN_CLASS) && ((mainClassMethod == null) || !Modifier.isPublic(mainClassMethod.flags()))) {
            transformedClass.produce(new BytecodeTransformerBuildItem(mainClassName, new MainMethodTransformer(index)));
        }

        return new MainClassBuildItem(mainClassName);
    }

    private static String sanitizeMainClassName(ClassInfo mainClass, IndexView index) {
        DotName mainClassDotName = mainClass.name();
        String className = mainClassDotName.toString();
        if (isKotlinClass(mainClass)) {
            MethodInfo mainMethod = mainClass.method("main",
                    ArrayType.create(Type.create(DotName.createSimple(String.class.getName()), Type.Kind.CLASS), 1));
            if (mainMethod == null) {
                boolean hasQuarkusApplicationInterface = index.getAllKnownImplementors(QUARKUS_APPLICATION).stream().map(
                        ClassInfo::name).anyMatch(d -> d.equals(mainClassDotName));
                if (!hasQuarkusApplicationInterface) {
                    className += "Kt";
                }
            }

        }
        return className;
    }

    private void generateMainForQuarkusApplication(String quarkusApplicationClassName,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        ClassCreator file = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClass, true), MAIN_CLASS, null,
                Object.class.getName());

        MethodCreator mv = file.getMethodCreator("main", void.class, String[].class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        mv.invokeStaticMethod(ofMethod(Quarkus.class, "run", void.class, Class.class, String[].class),
                mv.loadClassFromTCCL(quarkusApplicationClassName),
                mv.getMethodParam(0));
        mv.returnValue(null);
        file.close();
    }

    /**
     * Write a legacy bytecode recorder's output and emit the startup task dispatch.
     * <p>
     * This handles legacy {@link BytecodeRecorderImpl}-based build items. Transliterated action items
     * are handled inline by the loops in {@code build()} via the consolidated class dispatch.
     *
     * @param recorder the bytecode recorder (may be {@code null} or empty)
     * @param substitutions object substitution items
     * @param recordableConstructorBuildItems recordable constructor items
     * @param loaders object loader items
     * @param constants constant definition items
     * @param gizmoOutput the generated class output adaptor
     * @param startupContext the startup context result handle
     * @param bytecodeCreator the bytecode creator for emitting dispatch code
     */
    private void writeRecordedBytecode(BytecodeRecorderImpl recorder,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants,
            GeneratedClassGizmoAdaptor gizmoOutput,
            ResultHandle startupContext, BytecodeCreator bytecodeCreator) {

        if (recorder == null || recorder.isEmpty()) {
            return;
        }

        for (ObjectSubstitutionBuildItem sub : substitutions) {
            sub.holder.registerTo(recorder);
        }
        //noinspection removal
        for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
            //noinspection removal
            recorder.registerObjectLoader(item.getObjectLoader());
        }
        for (var item : recordableConstructorBuildItems) {
            recorder.markClassAsConstructorRecordable(item.getClazz());
        }
        for (BytecodeRecorderConstantDefinitionBuildItem constant : constants) {
            constant.register(recorder);
        }
        recorder.writeBytecode(gizmoOutput);

        ResultHandle dup = bytecodeCreator.newInstance(ofConstructor(recorder.getClassName()));
        bytecodeCreator.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup,
                startupContext);
        bytecodeCreator.invokeStaticMethod(PRINT_STEP_TIME_METHOD, startupContext);
    }

    /**
     * Record mapping a {@link TransliteratedAction} to its deploy method within a consolidated class.
     *
     * @param className the fully-qualified consolidated class name (dot-separated)
     * @param deployIndex the zero-based index of the deploy method within the consolidated class
     */
    private record ConsolidatedRef(String className, int deployIndex) {
    }

    /**
     * Collect all {@link TransliteratedAction}s from both static-init and runtime build items,
     * generate consolidated classes (one per phase), produce {@link GeneratedClassBuildItem}s,
     * and return a map from each action to its consolidated class and deploy method index.
     *
     * @param staticInitTasks the static-init build items
     * @param mainMethod the runtime build items
     * @param generatedClass the producer for generated class build items
     * @return an identity map from each transliterated action to its consolidated reference
     */
    private Map<TransliteratedAction, ConsolidatedRef> consolidateActions(
            List<StaticBytecodeRecorderBuildItem> staticInitTasks,
            List<MainBytecodeRecorderBuildItem> mainMethod,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {

        List<TransliteratedAction> staticActions = new ArrayList<>();
        for (StaticBytecodeRecorderBuildItem item : staticInitTasks) {
            TransliteratedAction ta = item.getTransliteratedAction();
            if (ta != null) {
                staticActions.add(ta);
            }
        }
        List<TransliteratedAction> runtimeActions = new ArrayList<>();
        for (MainBytecodeRecorderBuildItem item : mainMethod) {
            TransliteratedAction ta = item.getTransliteratedAction();
            if (ta != null) {
                runtimeActions.add(ta);
            }
        }

        if (staticActions.isEmpty() && runtimeActions.isEmpty()) {
            return Map.of();
        }

        Map<TransliteratedAction, ConsolidatedRef> refs = new IdentityHashMap<>();
        consolidateBatch(staticActions, "io/quarkus/generated/service/StaticServiceActions", refs, generatedClass);
        consolidateBatch(runtimeActions, "io/quarkus/generated/service/RuntimeServiceActions", refs, generatedClass);
        return refs;
    }

    /**
     * Generate a consolidated class for a batch of actions and register each action in the ref map.
     *
     * @param actions the list of transliterated actions to consolidate
     * @param classInternalName the JVM internal name for the consolidated class (slash-separated)
     * @param refs the identity map to populate with consolidated references
     * @param generatedClass the producer for generated class build items
     */
    private void consolidateBatch(
            List<TransliteratedAction> actions,
            String classInternalName,
            Map<TransliteratedAction, ConsolidatedRef> refs,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        if (actions.isEmpty()) {
            return;
        }
        Map<String, byte[]> classes = LambdaTransliterator.generateConsolidatedClass(classInternalName, actions);
        for (var entry : classes.entrySet()) {
            // service action classes reference application types in their bytecode,
            // so they must be application classes to share the same classloader
            generatedClass.produce(new GeneratedClassBuildItem(
                    true, entry.getKey().replace('/', '.'), entry.getValue()));
        }
        String className = classInternalName.replace('/', '.');
        for (int i = 0; i < actions.size(); i++) {
            refs.put(actions.get(i), new ConsolidatedRef(className, i));
        }
    }

    /**
     * Compute the set of service value keys that must survive between static-init and runtime-init.
     * This includes keys consumed by runtime-init service dependencies, runtime-init RuntimeValueWrapper
     * source keys, cross-phase recorder proxy keys, and CDI service-value bean keys.
     */
    private Set<String> computeCrossPhaseKeys(
            List<MainBytecodeRecorderBuildItem> runtimeTasks,
            List<ServiceValueRetentionBuildItem> retentionItems,
            Set<String> staticInitServiceKeys) {
        Set<String> keys = new HashSet<>();
        // keys from retention build items (proxy keys + CDI serviceValue keys)
        for (ServiceValueRetentionBuildItem item : retentionItems) {
            keys.addAll(item.keys());
        }
        // keys needed by cross-phase proxies: runtime-init service deps
        // that match a static-init service key (value read from serviceValues map)
        for (MainBytecodeRecorderBuildItem holder : runtimeTasks) {
            TransliteratedAction ta = holder.getTransliteratedAction();
            if (ta instanceof TransliteratedAction.ActionService as) {
                for (Dependency dep : as.dependencies()) {
                    if (dep.injected() && !dep.configDirect()) {
                        String key = LambdaTransliterator.serviceKey(dep.type(), dep.nameParts());
                        if (staticInitServiceKeys.contains(key)) {
                            keys.add(key);
                        }
                    }
                }
            }
        }
        return keys;
    }

    /**
     * Compute the set of service value keys that must survive after startup completes.
     * Only CDI service-value bean keys qualify (they are lazily accessed and self-draining).
     */
    private static Set<String> computePostStartupKeys(List<ServiceValueRetentionBuildItem> retentionItems) {
        Set<String> keys = new HashSet<>();
        for (ServiceValueRetentionBuildItem item : retentionItems) {
            if (item.neededAfterStartup()) {
                keys.addAll(item.keys());
            }
        }
        return keys;
    }

    /**
     * Generate a {@code startupContext.retainServiceValues(Set.of(k1, k2, ...))} call.
     */
    private static void generateRetainServiceValues(BytecodeCreator method, ResultHandle startupContext, Set<String> keys) {
        // load all keys as a String[]
        ResultHandle[] keyHandles = new ResultHandle[keys.size()];
        int i = 0;
        for (String key : keys) {
            keyHandles[i++] = method.load(key);
        }
        ResultHandle array = method.newArray(String.class, keyHandles.length);
        for (int j = 0; j < keyHandles.length; j++) {
            method.writeArrayValue(array, j, keyHandles[j]);
        }
        // Set.of(array) — since Set.of(Object[]) requires the varargs form
        ResultHandle keySet = method.invokeStaticInterfaceMethod(
                ofMethod(Set.class, "of", Set.class, Object[].class), array);
        method.invokeVirtualMethod(
                ofMethod(StartupContext.class, "retainServiceValues", void.class, Set.class),
                startupContext, keySet);
    }

    // ═══════════════════════════════════════════════
    // Service graph code generation
    //
    // TODO: migrate from Gizmo 1 (io.quarkus.gizmo) to Gizmo 2,
    //       which uses the ClassFile API (io.smallrye.classfile)
    //       internally. LambdaTransliterator already uses the ClassFile
    //       API directly; aligning here would reduce impedance mismatch
    //       with consolidated class generation.
    // ═══════════════════════════════════════════════

    /** Class descriptor for ServiceGraph. */
    private static final String SERVICE_GRAPH_CLASS = "io.quarkus.core.impl.ServiceGraph";
    /** Class descriptor for ServiceNode. */
    private static final String SERVICE_NODE_CLASS = "io.quarkus.core.impl.ServiceNode";

    /**
     * Emit bytecode to construct a {@link ServiceGraph} from a
     * {@link ServiceGraphBuilder.GraphPlan}.
     * <p>
     * The generated code creates a {@code ServiceGraph}, constructs all
     * {@code ServiceNode} instances in topological order (with sentinel
     * nodes at the boundaries), wires dependencies via constructor
     * parameters, and calls {@code graph.start()}.
     * <p>
     * Each node's action is a {@link MethodHandle} pointing
     * to either a deploy method in the consolidated class (for new services)
     * or a generated wrapper method (for legacy recorders and sentinels).
     *
     * @param code the bytecode creator to emit instructions into
     * @param plan the graph plan from {@link ServiceGraphBuilder}
     * @param classCreator the class creator for generating sentinel/wrapper methods
     * @param consolidatedRefs map from transliterated actions to their consolidated class references
     * @param gizmoOutput the output adaptor for legacy recorder class generation
     * @param substitutions legacy recorder substitutions
     * @param recordableConstructorBuildItems legacy recorder constructable items
     * @param loaders legacy recorder object loaders
     * @param constants legacy recorder constant definitions
     * @param phaseName "static" or "runtime", for generated method naming
     * @return the result handle for the constructed ServiceGraph
     */
    private ResultHandle emitServiceGraph(
            BytecodeCreator code,
            ServiceGraphBuilder.GraphPlan plan,
            ResultHandle startupContext,
            ClassCreator classCreator,
            Map<TransliteratedAction, ConsolidatedRef> consolidatedRefs,
            GeneratedClassGizmoAdaptor gizmoOutput,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants,
            String phaseName) {

        if (!plan.hasNodes()) {
            // empty plan: create a graph wrapping the existing startup context
            return code.newInstance(ofConstructor(SERVICE_GRAPH_CLASS, StartupContext.class), startupContext);
        }

        List<ServiceGraphBuilder.NodeDescriptor> nodes = plan.nodes();

        // create the ServiceGraph wrapping the shared startup context
        ResultHandle graph = code.newInstance(ofConstructor(SERVICE_GRAPH_CLASS, StartupContext.class), startupContext);

        // local variable array for node references (indexed by plan position)
        ResultHandle[] nodeHandles = new ResultHandle[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            ServiceGraphBuilder.NodeDescriptor node = nodes.get(i);
            int[] depIndices = node.dependencyIndices();

            // create the MethodHandle for this node's action
            ResultHandle mh = emitActionHandle(code, classCreator, node, consolidatedRefs,
                    gizmoOutput, substitutions, recordableConstructorBuildItems, loaders, constants,
                    phaseName, i);

            // create the ServiceNode using the appropriate constructor overload
            ResultHandle serviceNode;
            if (depIndices.length == 0) {
                serviceNode = code.newInstance(
                        ofConstructor(SERVICE_NODE_CLASS, String.class, MethodHandle.class,
                                SERVICE_GRAPH_CLASS, int.class),
                        code.load(node.name()), mh, graph, code.load(node.dependentCount()));
            } else if (depIndices.length == 1) {
                serviceNode = code.newInstance(
                        ofConstructor(SERVICE_NODE_CLASS, String.class, MethodHandle.class,
                                SERVICE_GRAPH_CLASS, int.class, SERVICE_NODE_CLASS),
                        code.load(node.name()), mh, graph, code.load(node.dependentCount()),
                        nodeHandles[depIndices[0]]);
            } else if (depIndices.length == 2) {
                serviceNode = code.newInstance(
                        ofConstructor(SERVICE_NODE_CLASS, String.class, MethodHandle.class,
                                SERVICE_GRAPH_CLASS, int.class, SERVICE_NODE_CLASS, SERVICE_NODE_CLASS),
                        code.load(node.name()), mh, graph, code.load(node.dependentCount()),
                        nodeHandles[depIndices[0]], nodeHandles[depIndices[1]]);
            } else {
                // build a List<ServiceNode> for the varargs constructor
                ResultHandle depArray = code.newArray(SERVICE_NODE_CLASS, depIndices.length);
                for (int j = 0; j < depIndices.length; j++) {
                    code.writeArrayValue(depArray, j, nodeHandles[depIndices[j]]);
                }
                ResultHandle depList = code.invokeStaticInterfaceMethod(
                        ofMethod(List.class, "of", List.class, Object[].class),
                        depArray);
                serviceNode = code.newInstance(
                        ofConstructor(SERVICE_NODE_CLASS, String.class, MethodHandle.class,
                                SERVICE_GRAPH_CLASS, int.class, List.class),
                        code.load(node.name()), mh, graph, code.load(node.dependentCount()), depList);
            }

            nodeHandles[i] = serviceNode;

            // set top/bottom sentinels on the graph
            if (i == plan.topIndex()) {
                code.invokeVirtualMethod(
                        ofMethod(SERVICE_GRAPH_CLASS, "setTop", void.class, SERVICE_NODE_CLASS),
                        graph, serviceNode);
            } else if (i == plan.bottomIndex()) {
                code.invokeVirtualMethod(
                        ofMethod(SERVICE_GRAPH_CLASS, "setBottom", void.class, SERVICE_NODE_CLASS),
                        graph, serviceNode);
            }
        }

        // start the graph (blocks until bottom sentinel signals completion)
        code.invokeVirtualMethod(ofMethod(SERVICE_GRAPH_CLASS, "start", void.class), graph);

        return graph;
    }

    /**
     * Emit bytecode to create the {@link MethodHandle} for a node's action.
     * <p>
     * For sentinel nodes, generates a static method in the Application class
     * and returns a handle to it. For service nodes, returns a handle to the
     * deploy method in the consolidated class. For legacy recorder nodes,
     * generates a wrapper method and returns a handle to it.
     *
     * @return a result handle for the MethodHandle constant
     */
    private ResultHandle emitActionHandle(
            BytecodeCreator code,
            ClassCreator classCreator,
            ServiceGraphBuilder.NodeDescriptor node,
            Map<TransliteratedAction, ConsolidatedRef> consolidatedRefs,
            GeneratedClassGizmoAdaptor gizmoOutput,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants,
            String phaseName,
            int nodeIndex) {

        String methodName;
        String targetClass;

        switch (node.kind()) {
            case SENTINEL -> {
                // generate a sentinel method in the Application class
                methodName = phaseName + "$sentinel$" + nodeIndex;
                targetClass = Application.APP_CLASS_NAME;
                boolean isTop = (nodeIndex == 0); // top sentinel is always index 0
                generateSentinelMethod(classCreator, methodName, isTop);
            }
            case LEGACY_RECORDER -> {
                // generate a legacy wrapper method in the Application class
                methodName = phaseName + "$legacy$" + nodeIndex;
                targetClass = Application.APP_CLASS_NAME;
                boolean usesShutdownContext = recorderUsesShutdownContext(node.recorders());
                generateLegacyWrapperMethod(classCreator, methodName, node.recorders(),
                        usesShutdownContext, gizmoOutput, substitutions,
                        recordableConstructorBuildItems, loaders, constants);
            }
            case SERVICE, ALIAS, RV_WRAPPER -> {
                // handle to deploy$N in the consolidated class
                ConsolidatedRef ref = consolidatedRefs.get(node.action());
                methodName = "deploy$" + ref.deployIndex();
                targetClass = ref.className();
            }
            case CROSS_PHASE_PROXY -> {
                // generate a proxy method that reads from the serviceValues map
                methodName = phaseName + "$crossphase$" + nodeIndex;
                targetClass = Application.APP_CLASS_NAME;
                generateCrossPhaseProxyMethod(classCreator, methodName, node.name());
            }
            default -> throw new IllegalStateException("Unknown node kind: " + node.kind());
        }

        // emit: MethodHandles.lookup().findStatic(targetClass, methodName, MethodType.methodType(void.class, ServiceNode.class))
        return emitFindStatic(code, targetClass, methodName);
    }

    /**
     * Emit bytecode to look up a static method handle with signature {@code (ServiceNode) → void}.
     *
     * @param code the bytecode creator
     * @param className the fully-qualified class name containing the method
     * @param methodName the method name
     * @return a result handle for the MethodHandle
     */
    private static ResultHandle emitFindStatic(BytecodeCreator code, String className, String methodName) {
        // MethodHandles.lookup()
        ResultHandle lookup = code.invokeStaticMethod(
                ofMethod(MethodHandles.class, "lookup",
                        MethodHandles.Lookup.class));
        // load target class
        ResultHandle targetClass = code.loadClassFromTCCL(className);
        // MethodType.methodType(void.class, ServiceNode.class)
        ResultHandle methodType = code.invokeStaticMethod(
                ofMethod(MethodType.class, "methodType",
                        MethodType.class, Class.class, Class.class),
                code.loadClassFromTCCL(void.class.getName()),
                code.loadClassFromTCCL(SERVICE_NODE_CLASS));
        // lookup.findStatic(targetClass, methodName, methodType)
        return code.invokeVirtualMethod(
                ofMethod(MethodHandles.Lookup.class, "findStatic",
                        MethodHandle.class, Class.class, String.class,
                        MethodType.class),
                lookup, targetClass, code.load(methodName), methodType);
    }

    /**
     * Generate a sentinel static method in the Application class.
     * <p>
     * Top sentinel: registers a stop handler that signals stop-done, then completes.
     * Bottom sentinel: signals start-done, then completes.
     *
     * @param classCreator the Application class creator
     * @param methodName the method name to generate
     * @param isTop {@code true} for top sentinel, {@code false} for bottom
     */
    private static void generateSentinelMethod(ClassCreator classCreator, String methodName, boolean isTop) {
        MethodCreator mc = classCreator.getMethodCreator(methodName, void.class, SERVICE_NODE_CLASS);
        mc.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        ResultHandle node = mc.getMethodParam(0);

        if (isTop) {
            // top sentinel: register stop handler that signals stop-done
            // node.onStop(() -> node.graph().signalStopDone())
            ResultHandle graph = mc.invokeVirtualMethod(
                    ofMethod(SERVICE_NODE_CLASS, "graph", SERVICE_GRAPH_CLASS), node);
            var stopRunnable = mc.createFunction(Runnable.class);
            BytecodeCreator stopBody = stopRunnable.getBytecode();
            // the graph is captured in the generated function's scope
            stopBody.invokeVirtualMethod(
                    ofMethod(SERVICE_GRAPH_CLASS, "signalStopDone", void.class), graph);
            stopBody.returnVoid();
            mc.invokeInterfaceMethod(
                    ofMethod("io.quarkus.core.StartContext", "onStop", void.class, Runnable.class),
                    node, stopRunnable.getInstance());
        } else {
            // bottom sentinel: signal start-done
            ResultHandle graph = mc.invokeVirtualMethod(
                    ofMethod(SERVICE_NODE_CLASS, "graph", SERVICE_GRAPH_CLASS), node);
            mc.invokeVirtualMethod(
                    ofMethod(SERVICE_GRAPH_CLASS, "signalStartDone", void.class), graph);
        }

        // both sentinels: void completion
        mc.invokeVirtualMethod(
                ofMethod(SERVICE_NODE_CLASS, "startComplete", void.class), node);
        mc.returnVoid();
    }

    /**
     * Generate a legacy recorder wrapper method in the Application class.
     * <p>
     * The generated method:
     * <ol>
     * <li>Optionally creates a {@link NodeShutdownContext} and registers
     * it as the node's stop handler</li>
     * <li>Gets the StartupContext from the graph</li>
     * <li>Instantiates and invokes each legacy recorder's StartupTask</li>
     * <li>Signals void completion</li>
     * </ol>
     *
     * @param classCreator the Application class creator
     * @param methodName the method name to generate
     * @param recorders the legacy bytecode recorders for this step
     * @param usesShutdownContext whether the recorders use ShutdownContext
     * @param gizmoOutput the output adaptor for recorder class generation
     * @param substitutions recorder substitutions
     * @param recordableConstructorBuildItems recorder constructable items
     * @param loaders recorder object loaders
     * @param constants recorder constant definitions
     */
    private void generateLegacyWrapperMethod(
            ClassCreator classCreator,
            String methodName,
            List<BytecodeRecorderImpl> recorders,
            boolean usesShutdownContext,
            GeneratedClassGizmoAdaptor gizmoOutput,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants) {

        MethodCreator mc = classCreator.getMethodCreator(methodName, void.class, SERVICE_NODE_CLASS);
        mc.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        ResultHandle node = mc.getMethodParam(0);

        // get StartupContext from graph
        ResultHandle graph = mc.invokeVirtualMethod(
                ofMethod(SERVICE_NODE_CLASS, "graph", SERVICE_GRAPH_CLASS), node);
        ResultHandle startupContext = mc.invokeVirtualMethod(
                ofMethod(SERVICE_GRAPH_CLASS, "startupContext", "io.quarkus.runtime.StartupContext"), graph);

        ResultHandle shutdownContext = null;
        if (usesShutdownContext) {
            // create per-node ShutdownContext and register as stop handler
            String nscClass = "io.quarkus.core.impl.NodeShutdownContext";
            shutdownContext = mc.newInstance(ofConstructor(nscClass));
            mc.invokeInterfaceMethod(
                    ofMethod("io.quarkus.core.StartContext", "onStop", void.class, Runnable.class),
                    node, shutdownContext);
            // make the ShutdownContext available to recorder-generated deploy() methods,
            // which read it from the StartupContext values map
            mc.invokeVirtualMethod(
                    ofMethod(StartupContext.class, "putValue", void.class, String.class, Object.class),
                    startupContext, mc.load(ShutdownContext.class.getName()), shutdownContext);
        }

        // write recorder bytecode and invoke deploy for each recorder in the chunk
        for (BytecodeRecorderImpl recorder : recorders) {
            if (recorder == null || recorder.isEmpty()) {
                continue;
            }
            for (ObjectSubstitutionBuildItem sub : substitutions) {
                sub.holder.registerTo(recorder);
            }
            //noinspection removal
            for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                //noinspection removal
                recorder.registerObjectLoader(item.getObjectLoader());
            }
            for (var item : recordableConstructorBuildItems) {
                recorder.markClassAsConstructorRecordable(item.getClazz());
            }
            for (BytecodeRecorderConstantDefinitionBuildItem constant : constants) {
                constant.register(recorder);
            }
            recorder.writeBytecode(gizmoOutput);

            // instantiate the generated StartupTask and call deploy
            ResultHandle task = mc.newInstance(ofConstructor(recorder.getClassName()));
            if (usesShutdownContext && shutdownContext != null) {
                // deploy(StartupContext, ShutdownContext)
                // TODO: BytecodeRecorderImpl needs to generate this 2-arg variant
                // For now, fall back to deploy(StartupContext) — ShutdownContext via map
                mc.invokeInterfaceMethod(
                        ofMethod("io.quarkus.runtime.StartupTask", "deploy", void.class,
                                "io.quarkus.runtime.StartupContext"),
                        task, startupContext);
            } else {
                mc.invokeInterfaceMethod(
                        ofMethod("io.quarkus.runtime.StartupTask", "deploy", void.class,
                                "io.quarkus.runtime.StartupContext"),
                        task, startupContext);
            }
        }

        // signal void completion
        mc.invokeVirtualMethod(
                ofMethod(SERVICE_NODE_CLASS, "startComplete", void.class), node);
        mc.returnVoid();
    }

    /**
     * Generate a cross-phase proxy method that reads a static-init service value
     * from the serviceValues map and signals typed completion.
     * <p>
     * This bridges a static-init service value into the runtime-init graph,
     * allowing runtime services to {@code require()} static-init services.
     *
     * @param classCreator the Application class creator
     * @param methodName the method name to generate
     * @param serviceKey the service key to read from the serviceValues map
     */
    private static void generateCrossPhaseProxyMethod(ClassCreator classCreator, String methodName, String serviceKey) {
        MethodCreator mc = classCreator.getMethodCreator(methodName, void.class, SERVICE_NODE_CLASS);
        mc.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        ResultHandle node = mc.getMethodParam(0);

        // get StartupContext from graph
        ResultHandle graph = mc.invokeVirtualMethod(
                ofMethod(SERVICE_NODE_CLASS, "graph", SERVICE_GRAPH_CLASS), node);
        ResultHandle startupContext = mc.invokeVirtualMethod(
                ofMethod(SERVICE_GRAPH_CLASS, "startupContext", "io.quarkus.runtime.StartupContext"), graph);

        // read from serviceValues map
        ResultHandle value = mc.invokeVirtualMethod(
                ofMethod(StartupContext.class, "getServiceValue", Object.class, String.class),
                startupContext, mc.load(serviceKey));

        // signal typed completion (value may be null for void static-init services)
        BranchResult isNull = mc.ifNull(value);
        BytecodeCreator trueBranch = isNull.trueBranch();
        trueBranch.invokeVirtualMethod(
                ofMethod(SERVICE_NODE_CLASS, "startComplete", void.class), node);
        trueBranch.returnVoid();
        BytecodeCreator falseBranch = isNull.falseBranch();
        falseBranch.invokeVirtualMethod(
                ofMethod(SERVICE_NODE_CLASS, "startComplete", void.class, Object.class), node, value);
        falseBranch.returnVoid();
    }

    /**
     * Check whether any of the given recorders use ShutdownContext.
     *
     * @param recorders the recorders to check
     * @return {@code true} if at least one recorder uses ShutdownContext
     */
    private static boolean recorderUsesShutdownContext(List<BytecodeRecorderImpl> recorders) {
        // TODO: BytecodeRecorderImpl should expose whether it references ShutdownContext.
        // For now, assume all recorders might use it (conservative).
        return true;
    }

    /**
     * registers the generated application class for reflection, needed when launching via the Quarkus launcher
     */
    @BuildStep
    ReflectiveClassBuildItem applicationReflection() {
        return ReflectiveClassBuildItem.builder(Application.APP_CLASS_NAME).reason("The generated application class").build();
    }

    /**
     * Transform the main class to support the launch protocol described in <a href="https://openjdk.org/jeps/445">JEP 445</a>.
     * Note that we can support this regardless of the JDK version running the application.
     */
    private static class MainMethodTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {

        private final IndexView index;

        public MainMethodTransformer(IndexView index) {
            this.index = index;
        }

        @Override
        public ClassVisitor apply(String mainClassName, ClassVisitor outputClassVisitor) {
            ClassInfo mainClassInfo = index.getClassByName(mainClassName);
            if (mainClassInfo == null) {
                throw new IllegalStateException(mainClassName + " should have a corresponding ClassInfo at this point");
            }
            ClassTransformer transformer = new ClassTransformer(mainClassName);
            Result result = doApply(mainClassName, outputClassVisitor, transformer, mainClassInfo);
            if (!result.isValid) {
                throw new RuntimeException(errorMessage(mainClassName));
            }
            if (result.classVisitor == null) {
                throw new IllegalStateException("result.classvisitor should not be null at this point");
            }
            return result.classVisitor;
        }

        private Result doApply(String originalMainClassName,
                ClassVisitor classVisitor, ClassTransformer transformer,
                ClassInfo currentClassInfo) {
            boolean isTopLevel = currentClassInfo.name().toString().equals(originalMainClassName);
            boolean allowStatic = isTopLevel;
            boolean hasStaticWithArgs = false;
            boolean hasStaticWithoutArgs = false;
            boolean hasInstanceWithArgs = false;
            boolean hasInstanceWithoutArgs = false;

            MethodInfo withArgs = currentClassInfo.method("main", STRING_ARRAY);
            MethodInfo withoutArgs = currentClassInfo.method("main");

            if (withArgs != null) {
                if (Modifier.isStatic(withArgs.flags())) {
                    if (allowStatic) {
                        hasStaticWithArgs = true;
                    }
                } else {
                    hasInstanceWithArgs = true;
                }
            }
            if (withoutArgs != null) {
                if (Modifier.isStatic(withoutArgs.flags())) {
                    if (allowStatic) {
                        hasStaticWithoutArgs = true;
                    }
                } else {
                    hasInstanceWithoutArgs = true;
                }
            }

            Result result;

            //impl NOTE: the sequence of boolean checks is very important as it follows what the JEP says is the proper sequence of method lookups
            if (hasStaticWithArgs) {
                if (Modifier.isPublic(withArgs.flags())) {
                    // nothing to do here
                    result = Result.valid(classVisitor);
                } else if (Modifier.isPrivate(withArgs.flags())) {
                    // the launch protocol says we can't use this one, but we still need to rename it to avoid conflicts with the potentially generated main
                    transformer.modifyMethod(MethodDescriptor.of(withArgs)).rename("$originalMain$");
                    result = Result.invalid(transformer.applyTo(classVisitor));
                } else {
                    // this is the simplest case where we just make the method public
                    transformer.modifyMethod(MethodDescriptor.of(withArgs)).removeModifiers(Modifier.PROTECTED)
                            .addModifiers(Modifier.PUBLIC);
                    result = Result.valid(transformer.applyTo(classVisitor));
                }
            } else if (hasStaticWithoutArgs) {
                if (Modifier.isPrivate(withoutArgs.flags())) {
                    // the launch protocol says we can't use this one
                    result = Result.invalid();
                    ;
                } else {
                    // we create a public static void(String[] args) method and all the target from it
                    MethodCreator standardMain = createStandardMain(transformer);
                    standardMain.invokeStaticMethod(MethodDescriptor.of(withoutArgs));
                    standardMain.returnValue(null);
                    result = Result.valid(transformer.applyTo(classVisitor));
                }
            } else if (hasInstanceWithArgs) {
                if (Modifier.isPrivate(withArgs.flags())) {
                    // the launch protocol says we can't use this one, but we still need to rename it to avoid conflicts with the potentially generated main
                    transformer.modifyMethod(MethodDescriptor.of(withArgs)).rename("$originalMain$");
                    result = Result.invalid(transformer.applyTo(classVisitor));
                } else {
                    // here we need to construct an instance and call the instance method with the args parameter
                    MethodCreator standardMain = createStandardMain(transformer);
                    ResultHandle instanceHandle = standardMain.newInstance(ofConstructor(originalMainClassName));
                    ResultHandle argsParamHandle = standardMain.getMethodParam(0);
                    if (isTopLevel) {
                        // we need to rename the method in order to avoid having two main methods with the same name
                        standardMain.invokeVirtualMethod(
                                ofMethod(originalMainClassName, "$$main$$", void.class, String[].class),
                                instanceHandle, argsParamHandle);

                        transformer.modifyMethod(MethodDescriptor.of(withArgs)).rename("$$main$$");
                    } else {
                        // Invoke super
                        standardMain.invokeSpecialMethod(withArgs, instanceHandle, argsParamHandle);
                    }
                    standardMain.returnValue(null);
                    result = Result.valid(transformer.applyTo(classVisitor));
                }
            } else if (hasInstanceWithoutArgs) {
                if (Modifier.isPrivate(withoutArgs.flags())) {
                    // the launch protocol says we can't use this one
                    result = Result.invalid();
                } else {
                    // here we need to construct an instance and call the instance method without any parameters
                    MethodCreator standardMain = createStandardMain(transformer);
                    ResultHandle instanceHandle = standardMain.newInstance(ofConstructor(originalMainClassName));
                    standardMain.invokeVirtualMethod(MethodDescriptor.of(withoutArgs), instanceHandle);
                    standardMain.returnValue(null);
                    result = Result.valid(transformer.applyTo(classVisitor));
                }
            } else {
                // this means that no main (with our without args was found)
                result = resultFromSuper(originalMainClassName, classVisitor, transformer, currentClassInfo);
            }
            if (!result.isValid) {
                // this means there were private main methods that we ignored
                result = resultFromSuper(originalMainClassName, classVisitor, transformer, currentClassInfo);
            }

            return result;
        }

        private Result resultFromSuper(String originalMainClassName, ClassVisitor outputClassVisitor,
                ClassTransformer transformer, ClassInfo currentClassInfo) {
            DotName superName = currentClassInfo.superName();
            if (superName.equals(OBJECT)) {
                // no valid main method was found
                return Result.invalid();
            }
            ClassInfo superClassInfo = index.getClassByName(superName);
            if (superClassInfo == null) {
                throw new IllegalStateException("Unable to find main method on class '" + originalMainClassName
                        + "' while it was also not possible to traverse the class hierarchy");
            }

            // check if the superclass has any valid candidates
            return doApply(originalMainClassName, outputClassVisitor, transformer, superClassInfo);
        }

        private static String errorMessage(String originalMainClassName) {
            return "Unable to find a valid main method on class '" + originalMainClassName
                    + "'. See https://openjdk.org/jeps/445 for details of what constitutes a valid main method.";
        }

        private static MethodCreator createStandardMain(ClassTransformer transformer) {
            return transformer.addMethod("main", void.class, String[].class)
                    .setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        }

        private static class Result {
            private final boolean isValid;
            private final ClassVisitor classVisitor;

            private Result(boolean isValid, ClassVisitor classVisitor) {
                this.isValid = isValid;
                this.classVisitor = classVisitor;
            }

            private static Result valid(ClassVisitor classVisitor) {
                return new Result(true, classVisitor);
            }

            private static Result invalid(ClassVisitor classVisitor) {
                return new Result(false, classVisitor);
            }

            private static Result invalid() {
                return new Result(false, null);
            }
        }
    }

    @BuildStep
    ReflectiveFieldBuildItem setupVersionField() {
        return new ReflectiveFieldBuildItem(
                "Ensure it's included in the executable to be able to grep the quarkus version",
                Application.APP_CLASS_NAME, QUARKUS_ANALYTICS_QUARKUS_VERSION);
    }
}
