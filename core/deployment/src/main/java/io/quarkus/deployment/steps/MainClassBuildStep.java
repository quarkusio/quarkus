package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.KotlinUtil.isKotlinClass;

import java.io.File;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
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

import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.bootstrap.naming.DisabledInitialContextManager;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.Version;
import io.quarkus.core.StartContext;
import io.quarkus.core.deployment.service.impl.Dependency;
import io.quarkus.core.deployment.service.impl.LambdaTransliterator;
import io.quarkus.core.deployment.service.impl.ServiceValueRetentionBuildItem;
import io.quarkus.core.deployment.service.impl.TransliteratedAction;
import io.quarkus.core.impl.JfrServiceMonitor;
import io.quarkus.core.impl.NoOpServiceMonitor;
import io.quarkus.core.impl.NodeShutdownContext;
import io.quarkus.core.impl.ServiceGraph;
import io.quarkus.core.impl.ServiceNode;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
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
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LambdaStrategy;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.ExecutionModeManager;
import io.quarkus.runtime.JVMUnsafeWarningsControl;
import io.quarkus.runtime.LaunchMode;
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

    /** Class descriptor for ServiceGraph. */
    private static final String SERVICE_GRAPH_CLASS = "io.quarkus.core.impl.ServiceGraph";
    /** Class descriptor for ServiceNode. */
    private static final String SERVICE_NODE_CLASS = "io.quarkus.core.impl.ServiceNode";

    // Class Descriptors (CD_)
    private static final ClassDesc CD_Application = ClassDesc.of(Application.class.getName());
    private static final ClassDesc CD_StartupContext = ClassDesc.of(StartupContext.class.getName());
    private static final ClassDesc CD_ServiceGraph = ClassDesc.of(SERVICE_GRAPH_CLASS);
    private static final ClassDesc CD_ServiceNode = ClassDesc.of(SERVICE_NODE_CLASS);
    private static final ClassDesc CD_LaunchMode = ClassDesc.of(LaunchMode.class.getName());

    private static final FieldDesc FD_InitialConfigurator_DELAYED_HANDLER = FieldDesc.of(
            ClassDesc.of("io.quarkus.bootstrap.logging.InitialConfigurator"),
            "DELAYED_HANDLER",
            ClassDesc.of("io.quarkus.bootstrap.logging.QuarkusDelayedHandler"));

    // MethodType Descriptors (MTD_)

    static {
        ClassDesc.of(Timing.class.getName());
        ClassDesc
                .of(RunTimeConfigurationGenerator.class.getName());
        ClassDesc.of(ValueRegistry.class.getName());
        ClassDesc.of(RuntimeInfoProvider.class.getName());
        ClassDesc.of(ConfigRuntimeSource.class.getName());
        ClassDesc.of(ConfigUtils.class.getName());
        ClassDesc.of(QuarkusDelayedHandler.class.getName());
        ClassDesc.of(DisabledInitialContextManager.class.getName());
        ClassDesc.of(JVMUnsafeWarningsControl.class.getName());
        MethodTypeDesc.of(ConstantDescs.CD_void);
        MethodTypeDesc.of(ConstantDescs.CD_void,
                ConstantDescs.CD_String.arrayType());
    }

    // Method / Constructor Descriptors (MD_ & CMD_)
    public static final MethodDesc MD_StepTiming_configureEnabled = MethodDesc.of(StepTiming.class, "configureEnabled",
            void.class);
    public static final MethodDesc MD_ExecutionModeManager_staticInit = MethodDesc.of(ExecutionModeManager.class, "staticInit",
            void.class);
    public static final MethodDesc MD_ExecutionModeManager_runtimeInit = MethodDesc.of(ExecutionModeManager.class,
            "runtimeInit", void.class);
    public static final MethodDesc MD_ExecutionModeManager_running = MethodDesc.of(ExecutionModeManager.class, "running",
            void.class);
    public static final MethodDesc MD_ExecutionModeManager_unset = MethodDesc.of(ExecutionModeManager.class, "unset",
            void.class);
    public static final MethodDesc MD_StepTiming_configureStart = MethodDesc.of(StepTiming.class, "configureStart", void.class);

    private static final MethodDesc MD_DisabledInitialContextManager_register = MethodDesc
            .of(DisabledInitialContextManager.class, "register", void.class);
    private static final MethodDesc MD_JVMUnsafeWarningsControl_disableUnsafeRelatedWarnings = MethodDesc
            .of(JVMUnsafeWarningsControl.class, "disableUnsafeRelatedWarnings", void.class);
    private static final MethodDesc MD_System_setProperty = MethodDesc.of(System.class, "setProperty", String.class,
            String.class, String.class);
    private static final MethodDesc MD_Class_forName = MethodDesc.of(Class.class, "forName", Class.class, String.class,
            boolean.class, ClassLoader.class);
    private static final MethodDesc MD_Thread_currentThread = MethodDesc.of(Thread.class, "currentThread", Thread.class);
    private static final MethodDesc MD_Thread_getContextClassLoader = MethodDesc.of(Thread.class, "getContextClassLoader",
            ClassLoader.class);
    private static final MethodDesc MD_Timing_staticInitStarted = MethodDesc.of(Timing.class, "staticInitStarted", void.class,
            boolean.class);
    private static final MethodDesc MD_Timing_mainStarted = MethodDesc.of(Timing.class, "mainStarted", void.class);
    private static final MethodDesc MD_Timing_printStartupTime = MethodDesc.of(Timing.class, "printStartupTime", void.class,
            String.class, String.class, String.class, String.class, List.class, boolean.class, boolean.class);
    private static final MethodDesc MD_Logger_getLogger = MethodDesc.of(Logger.class, "getLogger", Logger.class, String.class);
    private static final MethodDesc MD_Application_getValueRegistry = MethodDesc.of(Application.class, "getValueRegistry",
            ValueRegistry.class);
    private static final MethodDesc MD_StartupContext_putValue = MethodDesc.of(StartupContext.class, "putValue", void.class,
            String.class, Object.class);
    private static final MethodDesc MD_StartupContext_setCommandLineArguments = MethodDesc.of(StartupContext.class,
            "setCommandLineArguments", void.class, String[].class);
    private static final MethodDesc MD_StartupContext_getServiceValue = MethodDesc.of(StartupContext.class, "getServiceValue",
            Object.class, String.class);
    private static final MethodDesc MD_StartupContext_clearServiceValues = MethodDesc.of(StartupContext.class,
            "clearServiceValues", void.class);
    private static final MethodDesc MD_StartupContext_close = MethodDesc.of(StartupContext.class, "close", void.class);
    private static final MethodDesc MD_System_getProperty = MethodDesc.of(System.class, "getProperty", String.class,
            String.class);
    private static final MethodDesc MD_StringBuilder_length = MethodDesc.of(StringBuilder.class, "length", int.class);
    private static final MethodDesc MD_StringBuilder_append = MethodDesc.of(StringBuilder.class, "append", StringBuilder.class,
            String.class);
    private static final MethodDesc MD_StringBuilder_toString = MethodDesc.of(StringBuilder.class, "toString", String.class);
    private static final MethodDesc MD_RuntimeInfoProvider_register = MethodDesc.of(RuntimeInfoProvider.class, "register",
            void.class, ValueRegistry.class, RuntimeSource.class);
    private static final MethodDesc MD_ConfigRuntimeSource_runtimeSource = MethodDesc.of(ConfigRuntimeSource.class,
            "runtimeSource", RuntimeSource.class);
    private static final MethodDesc MD_String_join = MethodDesc.of(String.class, "join", String.class, CharSequence.class,
            Iterable.class);
    private static final MethodDesc MD_ConfigUtils_getProfiles = MethodDesc.of(ConfigUtils.class, "getProfiles", List.class);
    private static final MethodDesc MD_QuarkusConsole_start = MethodDesc.of(QuarkusConsole.class, "start", void.class);
    private static final MethodDesc MD_QuarkusDelayedHandler_isActivated = MethodDesc.of(QuarkusDelayedHandler.class,
            "isActivated", boolean.class);
    private static final MethodDesc MD_QuarkusDelayedHandler_setHandlers = MethodDesc.of(QuarkusDelayedHandler.class,
            "setHandlers", Handler[].class, Handler[].class);
    private static final MethodDesc MD_ApplicationStateNotification_notifyStartupFailed = MethodDesc
            .of(ApplicationStateNotification.class, "notifyStartupFailed", void.class, Throwable.class);
    private static final MethodDesc MD_ServiceGraph_stop = MethodDesc.of(ServiceGraph.class, "stop", void.class);
    private static final MethodDesc MD_ServiceGraph_start = MethodDesc.of(ServiceGraph.class, "start", void.class);
    private static final MethodDesc MD_ServiceGraph_setTop = MethodDesc.of(ServiceGraph.class, "setTop", void.class,
            ServiceNode.class);
    private static final MethodDesc MD_ServiceGraph_setBottom = MethodDesc.of(ServiceGraph.class, "setBottom", void.class,
            ServiceNode.class);
    private static final MethodDesc MD_ServiceGraph_signalStopDone = MethodDesc.of(ServiceGraph.class, "signalStopDone",
            void.class);
    private static final MethodDesc MD_ServiceGraph_signalStartDone = MethodDesc.of(ServiceGraph.class, "signalStartDone",
            void.class);
    private static final MethodDesc MD_ServiceNode_graph = MethodDesc.of(ServiceNode.class, "graph", ServiceGraph.class);
    private static final MethodDesc MD_ServiceNode_startComplete = MethodDesc.of(ServiceNode.class, "startComplete",
            void.class);
    private static final MethodDesc MD_ServiceNode_startComplete_Object = MethodDesc.of(ServiceNode.class, "startComplete",
            void.class, Object.class);

    private static final MethodDesc MD_Config_staticInitConfig = ClassMethodDesc
            .of(ClassDesc.of("io.quarkus.runtime.generated.Config"), "staticInitConfig", void.class);
    private static final MethodDesc MD_Config_runtimeConfig = ClassMethodDesc
            .of(ClassDesc.of("io.quarkus.runtime.generated.Config"), "runtimeConfig", void.class, ValueRegistry.class);
    private static final MethodDesc MD_PreInitRunner_executePreInitTasks = ClassMethodDesc
            .of(ClassDesc.of("io.quarkus.runtime.generated.PreInitRunner"), "executePreInitTasks", void.class);

    private static final ConstructorDesc CMD_StartupContext_ctor = ConstructorDesc.of(CD_StartupContext);
    private static final ConstructorDesc CMD_NodeShutdownContext_ctor_boolean = ConstructorDesc
            .of(ClassDesc.of(NodeShutdownContext.class.getName()), boolean.class);
    private static final ConstructorDesc CMD_StringBuilder_ctor_String = ConstructorDesc.of(StringBuilder.class, String.class);
    private static final ConstructorDesc CMD_ServiceGraph_ctor_StartupContext_ServiceMonitor = ConstructorDesc
            .of(CD_ServiceGraph, CD_StartupContext, ClassDesc.of("io.quarkus.core.impl.ServiceMonitor"));
    private static final ConstructorDesc CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int = ConstructorDesc.of(
            CD_ServiceNode, ConstantDescs.CD_String, ClassDesc.of(MethodHandle.class.getName()), CD_ServiceGraph,
            ConstantDescs.CD_int);
    private static final ConstructorDesc CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int_ServiceNode = ConstructorDesc
            .of(CD_ServiceNode, ConstantDescs.CD_String, ClassDesc.of(MethodHandle.class.getName()), CD_ServiceGraph,
                    ConstantDescs.CD_int, CD_ServiceNode);
    private static final ConstructorDesc CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int_ServiceNode_ServiceNode = ConstructorDesc
            .of(CD_ServiceNode, ConstantDescs.CD_String, ClassDesc.of(MethodHandle.class.getName()), CD_ServiceGraph,
                    ConstantDescs.CD_int, CD_ServiceNode, CD_ServiceNode);
    private static final ConstructorDesc CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int_List = ConstructorDesc.of(
            CD_ServiceNode, ConstantDescs.CD_String, ClassDesc.of(MethodHandle.class.getName()), CD_ServiceGraph,
            ConstantDescs.CD_int, ClassDesc.of(List.class.getName()));

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
            List<ServiceValueRetentionBuildItem> retentionItems,
            Capabilities capabilities) {

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
        GeneratedClassGizmo2Adaptor gizmoOutput = new GeneratedClassGizmo2Adaptor(generatedClass, null, null, true);
        Gizmo g = Gizmo.create(gizmoOutput).withLambdaStrategy(LambdaStrategy.CLASSIC);

        g.class_(Application.APP_CLASS_NAME, file -> {
            file.extends_(Application.class);

            // LOG static field
            StaticFieldVar logField = file.staticField(LOG, fc -> {
                fc.private_();
                fc.setType(Logger.class);
            });

            // STARTUP_CONTEXT static field
            StaticFieldVar scField = file.staticField(STARTUP_CONTEXT, fc -> {
                fc.public_();
                fc.setType(StartupContext.class);
            });

            // static fields for service graphs (used by doStop)
            StaticFieldVar staticGraphField = file.staticField("STATIC_GRAPH", fc -> {
                fc.private_();
                fc.setType(ServiceGraph.class);
            });

            StaticFieldVar runtimeGraphField = file.staticField("RUNTIME_GRAPH", fc -> {
                fc.private_();
                fc.setType(ServiceGraph.class);
            });

            StaticFieldVar quarkusVersionField = file.staticField(QUARKUS_ANALYTICS_QUARKUS_VERSION, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(String.class);
            });

            file.constructor(ctor -> ctor.body(b0 -> {
                b0.invokeSpecial(ConstructorDesc.of(CD_Application, boolean.class), file.this_(),
                        Const.of(launchMode.isAuxiliaryApplication()));
                b0.return_();
            }));

            file.staticInitializer(b0 -> {
                if (!namingConfig.enableJndi() && allowJNDIBuildItems.isEmpty()) {
                    b0.invokeStatic(MD_DisabledInitialContextManager_register);
                }
                b0.invokeStatic(MD_JVMUnsafeWarningsControl_disableUnsafeRelatedWarnings);

                // very first thing is to set system props (for build time)
                // make sure we record the system properties in order for build reproducibility
                for (SystemPropertyBuildItem i : properties.stream()
                        .sorted(Comparator.comparing(SystemPropertyBuildItem::getKey)).toList()) {
                    b0.invokeStatic(MD_System_setProperty, Const.of(i.getKey()), Const.of(i.getValue()));
                }

                if (preInitBuildItem.isPresent()) {
                    // we need to initialize JBoss Logging before starting any parallel work, it's too central
                    Expr tccl = b0.invokeVirtual(MD_Thread_getContextClassLoader, b0.invokeStatic(MD_Thread_currentThread));
                    b0.invokeStatic(MD_Class_forName, Const.of("org.jboss.logging.LoggerProviders"), Const.of(true), tccl);

                    // then we can preinitialize
                    b0.invokeStatic(MD_PreInitRunner_executePreInitTasks);
                }

                //set the launch mode
                b0.invokeStatic(MethodDesc.of(LaunchMode.class, "set", void.class, LaunchMode.class),
                        Expr.staticField(FieldDesc.of(CD_LaunchMode, launchMode.getLaunchMode().name(), CD_LaunchMode)));

                b0.invokeStatic(MD_StepTiming_configureEnabled);
                b0.invokeStatic(MD_ExecutionModeManager_staticInit);

                b0.invokeStatic(MD_Timing_staticInitStarted, Const.of(launchMode.isAuxiliaryApplication()));

                // Create Static Init Config and associate it with the current classloader
                b0.invokeStatic(MD_Config_staticInitConfig);

                // Init the LOG instance
                b0.set(logField, b0.invokeStatic(MD_Logger_getLogger, Const.of("io.quarkus.application")));

                // Init the __QUARKUS_ANALYTICS_QUARKUS_VERSION field
                b0.set(quarkusVersionField, Const.of("__quarkus_analytics__quarkus.version=" + Version.getVersion()));

                b0.invokeStatic(MD_StepTiming_configureStart);

                b0.try_(t1 -> {
                    t1.body(b2 -> {
                        // create the shared StartupContext (used by both static-init and runtime-init graphs)
                        LocalVar sc = b2.localVar("sc", b2.new_(CMD_StartupContext_ctor));
                        b2.set(scField, sc);

                        // build and start the static-init service graph
                        LocalVar staticGraph = emitServiceGraph(b2, staticPlan, sc, file, consolidatedRefs,
                                generatedClass, substitutions, recordableConstructorBuildItems, loaders, constants, "static",
                                capabilities);
                        b2.set(staticGraphField, staticGraph);

                        // discard static-init-only service values; retain keys needed at runtime-init
                        Set<String> crossPhaseKeys = computeCrossPhaseKeys(mainMethod, retentionItems,
                                staticPlan.serviceKeys());
                        if (!crossPhaseKeys.isEmpty()) {
                            generateRetainServiceValues(b2, sc, crossPhaseKeys);
                        }
                    });
                    t1.catch_(Throwable.class, "t", (b2, t) -> {
                        b2.invokeStatic(MD_ApplicationStateNotification_notifyStartupFailed, t);
                        // on failure, stop the static-init graph if it was started
                        LocalVar failGraph = b2.localVar("failGraph", b2.get(staticGraphField));
                        b2.if_(b2.ne(failGraph, Const.ofNull(CD_ServiceGraph)),
                                b3 -> b3.invokeVirtual(MD_ServiceGraph_stop, failGraph));
                        b2.throw_(b2.new_(
                                ConstructorDesc.of(ClassDesc.of(RuntimeException.class.getName()),
                                        ClassDesc.of(String.class.getName()), ClassDesc.of(Throwable.class.getName())),
                                Const.of("Failed to start quarkus"), t));
                    });
                });
                b0.return_();
            });

            // Application class: start method
            file.method("doStart", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String.arrayType()), mc -> {
                mc.protected_();
                mc.final_();
                ParamVar args = mc.parameter("args");
                mc.body(b0 -> {
                    LocalVar sc = b0.localVar("sc", b0.get(scField));

                    // Register ValueRegistry with StartupContext, so it can be injected into Recorders
                    LocalVar valueRegistry = b0.localVar("valueRegistry",
                            b0.invokeVirtual(MD_Application_getValueRegistry, file.this_()));
                    b0.invokeVirtual(MD_StartupContext_putValue, sc, Const.of(ValueRegistry.class.getName()), valueRegistry);

                    // Make sure we set properties in doStartup as well. This is necessary because setting them in the static-init
                    // sets them at build-time, on the host JVM, while SVM has substitutions for System. get/setProperty at
                    // run-time which will never see those properties unless we also set them at run-time.
                    // make sure we record the system properties in order for build reproducibility
                    for (SystemPropertyBuildItem i : properties.stream()
                            .sorted(Comparator.comparing(SystemPropertyBuildItem::getKey)).toList()) {
                        b0.invokeStatic(MD_System_setProperty, Const.of(i.getKey()), Const.of(i.getValue()));
                    }
                    // make sure we record the system properties in order for build reproducibility
                    for (GeneratedRuntimeSystemPropertyBuildItem i : generatedRuntimeSystemProperties.stream()
                            .sorted(Comparator.comparing(GeneratedRuntimeSystemPropertyBuildItem::getKey)).toList()) {
                        b0.invokeStatic(MD_System_setProperty, Const.of(i.getKey()),
                                b0.invokeVirtual(ClassMethodDesc.of(ClassDesc.of(i.getGeneratorClass()), "get", String.class),
                                        b0.new_(ConstructorDesc.of(ClassDesc.of(i.getGeneratorClass())))));
                    }
                    b0.invokeStatic(MD_ExecutionModeManager_runtimeInit);

                    // Set the SSL system properties
                    if (!javaLibraryPathAdditionalPaths.isEmpty()) {
                        LocalVar javaLibraryPath = b0.localVar("javaLibraryPath", b0.new_(CMD_StringBuilder_ctor_String,
                                b0.invokeStatic(MD_System_getProperty, Const.of(JAVA_LIBRARY_PATH))));
                        for (JavaLibraryPathAdditionalPathBuildItem javaLibraryPathAdditionalPath : javaLibraryPathAdditionalPaths) {
                            LocalVar javaLibraryPathLength = b0.localVar("javaLibraryPathLength",
                                    b0.invokeVirtual(MD_StringBuilder_length, javaLibraryPath));
                            b0.if_(b0.ne(javaLibraryPathLength, 0), b1 -> b1.invokeVirtual(MD_StringBuilder_append,
                                    javaLibraryPath, Const.of(File.pathSeparator)));
                            b0.invokeVirtual(MD_StringBuilder_append, javaLibraryPath,
                                    Const.of(javaLibraryPathAdditionalPath.getPath()));
                        }
                        b0.invokeStatic(MD_System_setProperty, Const.of(JAVA_LIBRARY_PATH),
                                b0.invokeVirtual(MD_StringBuilder_toString, javaLibraryPath));
                    }

                    b0.invokeStatic(MD_Timing_mainStarted);

                    //now set the command line arguments
                    b0.invokeVirtual(MD_StartupContext_setCommandLineArguments, sc, args);

                    b0.invokeStatic(MD_StepTiming_configureEnabled);

                    b0.try_(t1 -> {
                        t1.body(b2 -> {
                            // Create Runtime Config and associate it with the current classloader
                            b2.invokeStatic(MD_Config_runtimeConfig, valueRegistry);

                            // Register RuntimeInfoProviders with ValueRegistry
                            for (ValueRegistryRuntimeInfoProviderBuildItem runtimeInfoProviderClass : runtimeInfoProviders) {
                                LocalVar runtimeInfoProvider = b2.localVar("runtimeInfoProvider", b2.new_(ConstructorDesc
                                        .of(ClassDesc.of(runtimeInfoProviderClass.getRuntimeInfoProvider().getName()))));
                                b2.invokeInterface(MD_RuntimeInfoProvider_register, runtimeInfoProvider, valueRegistry,
                                        b2.invokeStatic(MD_ConfigRuntimeSource_runtimeSource));
                            }

                            // build and start the runtime service graph (shares the same StartupContext)
                            LocalVar runtimeGraph = emitServiceGraph(b2, runtimePlan, sc, file, consolidatedRefs,
                                    generatedClass, substitutions, recordableConstructorBuildItems, loaders, constants,
                                    "runtime", capabilities);
                            b2.set(runtimeGraphField, runtimeGraph);

                            // discard runtime-init service values; retain only CDI service-value bean keys
                            // (those are self-draining: each bean's creation function calls removeServiceValue)
                            Set<String> postStartupKeys = computePostStartupKeys(retentionItems);
                            if (postStartupKeys.isEmpty()) {
                                b2.invokeVirtual(MD_StartupContext_clearServiceValues, sc);
                            } else {
                                generateRetainServiceValues(b2, sc, postStartupKeys);
                            }

                            b2.invokeStatic(MD_ExecutionModeManager_running);

                            // Startup log messages
                            List<String> featureNames = new ArrayList<>();
                            for (FeatureBuildItem feature : features) {
                                if (!featureNames.contains(feature.getName())) {
                                    featureNames.add(feature.getName());
                                }
                            }
                            LocalVar featuresHandle = b2.localVar("featuresHandle",
                                    b2.listOf(featureNames.stream().sorted().map(Const::of).toArray(Expr[]::new)));
                            b2.invokeStatic(MD_Timing_printStartupTime,
                                    Const.of(applicationInfo.getName()),
                                    Const.of(applicationInfo.getVersion()),
                                    Const.of(Version.getVersion()),
                                    b2.invokeStatic(MD_String_join, Const.of(", "), featuresHandle),
                                    b2.invokeStatic(MD_ConfigUtils_getProfiles),
                                    Const.of(LaunchMode.DEVELOPMENT.equals(launchMode.getLaunchMode())),
                                    Const.of(launchMode.isAuxiliaryApplication()));

                            b2.invokeStatic(MD_QuarkusConsole_start);
                        });
                        t1.catch_(PreventFurtherStepsException.class, "pf", (b2, pf) -> {
                            // stop both graphs to run shutdown handlers before closing the context
                            LocalVar pfRtGraph = b2.localVar("pfRtGraph", b2.get(runtimeGraphField));
                            b2.if_(b2.ne(pfRtGraph, Const.ofNull(CD_ServiceGraph)),
                                    b3 -> b3.invokeVirtual(MD_ServiceGraph_stop, pfRtGraph));
                            LocalVar pfStGraph = b2.localVar("pfStGraph", b2.get(staticGraphField));
                            b2.if_(b2.ne(pfStGraph, Const.ofNull(CD_ServiceGraph)),
                                    b3 -> b3.invokeVirtual(MD_ServiceGraph_stop, pfStGraph));
                            b2.invokeVirtual(MD_StartupContext_close, sc);
                        });
                        t1.catch_(Throwable.class, "t", (b2, t) -> {
                            // an exception was thrown before logging was actually setup, we simply dump everything to the console
                            // we don't do this for dev mode, as on startup failure dev mode sets up its own logging
                            if (launchMode.getLaunchMode() != LaunchMode.DEVELOPMENT) {
                                LocalVar delayedHandler = b2.localVar("delayedHandler",
                                        Expr.staticField(FD_InitialConfigurator_DELAYED_HANDLER));
                                LocalVar isActivated = b2.localVar("isActivated",
                                        b2.invokeVirtual(MD_QuarkusDelayedHandler_isActivated, delayedHandler));
                                b2.if_(b2.eq(isActivated, 0), b3 -> {
                                    LocalVar handlersArray = b3.localVar("handlersArray",
                                            b3.newEmptyArray(ClassDesc.of(Handler.class.getName()), 1));
                                    b3.set(handlersArray.elem(0), b3.new_(ConstructorDesc.of(ConsoleHandler.class)));
                                    b3.invokeVirtual(MD_QuarkusDelayedHandler_setHandlers, delayedHandler, handlersArray);
                                });
                            }

                            // stop both graphs to run shutdown handlers (release Vertx, thread pools, etc.)
                            LocalVar failRtGraph = b2.localVar("failRtGraph", b2.get(runtimeGraphField));
                            b2.if_(b2.ne(failRtGraph, Const.ofNull(CD_ServiceGraph)),
                                    b3 -> b3.invokeVirtual(MD_ServiceGraph_stop, failRtGraph));
                            LocalVar failStGraph = b2.localVar("failStGraph", b2.get(staticGraphField));
                            b2.if_(b2.ne(failStGraph, Const.ofNull(CD_ServiceGraph)),
                                    b3 -> b3.invokeVirtual(MD_ServiceGraph_stop, failStGraph));
                            b2.invokeVirtual(MD_StartupContext_close, sc);
                            b2.throw_(b2.new_(
                                    ConstructorDesc.of(ClassDesc.of(RuntimeException.class.getName()),
                                            ClassDesc.of(String.class.getName()), ClassDesc.of(Throwable.class.getName())),
                                    Const.of("Failed to start quarkus"), t));
                        });
                    });
                    b0.return_();
                });
            });

            // Application class: stop method
            file.method("doStop", MethodTypeDesc.of(ConstantDescs.CD_void), mc -> {
                mc.protected_();
                mc.final_();
                mc.body(b0 -> {
                    b0.invokeStatic(MD_ExecutionModeManager_unset);
                    // stop the runtime graph, then the static-init graph
                    LocalVar rtGraph = b0.localVar("rtGraph", b0.get(runtimeGraphField));
                    b0.if_(b0.ne(rtGraph, Const.ofNull(CD_ServiceGraph)),
                            b1 -> b1.invokeVirtual(MD_ServiceGraph_stop, rtGraph));
                    b0.set(runtimeGraphField, Const.ofNull(CD_ServiceGraph));

                    LocalVar stGraph = b0.localVar("stGraph", b0.get(staticGraphField));
                    b0.if_(b0.ne(stGraph, Const.ofNull(CD_ServiceGraph)),
                            b1 -> b1.invokeVirtual(MD_ServiceGraph_stop, stGraph));
                    b0.set(staticGraphField, Const.ofNull(CD_ServiceGraph));

                    // close and release the StartupContext (clears values/serviceValues maps)
                    LocalVar sc = b0.localVar("sc", b0.get(scField));
                    b0.if_(b0.ne(sc, Const.ofNull(CD_StartupContext)), b1 -> b1.invokeVirtual(MD_StartupContext_close, sc));
                    b0.set(scField, Const.ofNull(CD_StartupContext));

                    b0.return_();
                });
            });

            // getName method
            file.method("getName", MethodTypeDesc.of(ConstantDescs.CD_String), mc -> {
                mc.public_();
                mc.body(b0 -> b0.return_(Const.of(applicationInfo.getName())));
            });

            // hasStaticGraph method
            file.method("hasStaticGraph", MethodTypeDesc.of(ConstantDescs.CD_boolean), mc -> {
                mc.public_();
                mc.final_();
                mc.body(b0 -> b0.return_(b0.ne(b0.get(staticGraphField), Const.ofNull(CD_ServiceGraph))));
            });
        });
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
                io.quarkus.gizmo.ClassCreator file = new io.quarkus.gizmo.ClassCreator(
                        new GeneratedClassGizmoAdaptor(generatedClass, true), MAIN_CLASS, null,
                        Object.class.getName());

                MethodCreator mv = file.getMethodCreator("main", void.class, String[].class);
                mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
                mv.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Quarkus.class, "run", void.class, String[].class),
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
                                + "' does not correspond to either a qualified class name or a matching 'name' field of one of the '@QuarkusMain' annotations");
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
        GeneratedClassGizmo2Adaptor output = new GeneratedClassGizmo2Adaptor(generatedClass, null, null, true);
        Gizmo.create(output).class_(MAIN_CLASS, file -> file.staticMethod("main",
                MethodTypeDesc.of(ConstantDescs.CD_void, ClassDesc.of(String.class.getName()).arrayType()), mc -> {
                    mc.public_();
                    ParamVar args = mc.parameter("args");
                    mc.body(b0 -> {
                        b0.invokeStatic(MethodDesc.of(Quarkus.class, "run", void.class, Class.class, String[].class),
                                Const.of(ClassDesc.of(quarkusApplicationClassName)), args);
                        b0.return_();
                    });
                }));
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
    private static void generateRetainServiceValues(BlockCreator b0, Expr sc, Set<String> keys) {
        Expr array = b0.newArray(ConstantDescs.CD_String, keys.stream().map(Const::of).toArray(Expr[]::new));
        Expr keySet = b0.invokeStatic(MethodDesc.of(Set.class, "of", Set.class, Object[].class), array);
        b0.invokeVirtual(MethodDesc.of(StartupContext.class, "retainServiceValues", void.class, Set.class),
                sc, keySet);
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
     * @param sc the startup context expression
     * @param file the class creator for generating sentinel/wrapper methods
     * @param consolidatedRefs map from transliterated actions to their consolidated class references
     * @param generatedClass the producer for generated class build items
     * @param substitutions legacy recorder substitutions
     * @param recordableConstructorBuildItems legacy recorder constructable items
     * @param loaders legacy recorder object loaders
     * @param constants legacy recorder constant definitions
     * @param phaseName "static" or "runtime", for generated method naming
     * @param capabilities the capabilities build item
     * @return the result handle for the constructed ServiceGraph
     */
    private LocalVar emitServiceGraph(
            BlockCreator code,
            ServiceGraphBuilder.GraphPlan plan,
            Expr sc,
            ClassCreator file,
            Map<TransliteratedAction, ConsolidatedRef> consolidatedRefs,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants,
            String phaseName,
            Capabilities capabilities) {

        LocalVar monitor = code.localVar("monitor", capabilities.isPresent(Capability.JFR)
                ? Expr.staticField(
                        FieldDesc.of(ClassDesc.of(JfrServiceMonitor.class.getName()), "INSTANCE", JfrServiceMonitor.class))
                : Expr.staticField(
                        FieldDesc.of(ClassDesc.of(NoOpServiceMonitor.class.getName()), "INSTANCE", NoOpServiceMonitor.class)));

        if (!plan.hasNodes()) {
            return code.localVar("emptyGraph", code.new_(CMD_ServiceGraph_ctor_StartupContext_ServiceMonitor, sc, monitor));
        }

        List<ServiceGraphBuilder.NodeDescriptor> nodes = plan.nodes();

        LocalVar graph = code.localVar("graph", code.new_(CMD_ServiceGraph_ctor_StartupContext_ServiceMonitor, sc, monitor));

        LocalVar[] nodeHandles = new LocalVar[nodes.size()];

        for (int i = 0; i < nodes.size(); i++) {
            ServiceGraphBuilder.NodeDescriptor node = nodes.get(i);
            int[] depIndices = node.dependencyIndices();

            LocalVar mh = emitActionHandle(code, file, node, consolidatedRefs,
                    generatedClass, substitutions, recordableConstructorBuildItems, loaders, constants,
                    phaseName, i);

            LocalVar serviceNode;
            if (depIndices.length == 0) {
                serviceNode = code.localVar("node$" + i, code.new_(CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int,
                        Const.of(node.name()), mh, graph, Const.of(node.dependentCount())));
            } else if (depIndices.length == 1) {
                serviceNode = code.localVar("node$" + i,
                        code.new_(CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int_ServiceNode,
                                Const.of(node.name()), mh, graph, Const.of(node.dependentCount()),
                                nodeHandles[depIndices[0]]));
            } else if (depIndices.length == 2) {
                serviceNode = code.localVar("node$" + i,
                        code.new_(CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int_ServiceNode_ServiceNode,
                                Const.of(node.name()), mh, graph, Const.of(node.dependentCount()),
                                nodeHandles[depIndices[0]], nodeHandles[depIndices[1]]));
            } else {
                Expr depArray = code.newArray(CD_ServiceNode,
                        Arrays.stream(depIndices).mapToObj(idx -> nodeHandles[idx]).toArray(Expr[]::new));
                Expr depList = code.invokeStatic(MethodDesc.of(List.class, "of", List.class, Object[].class), depArray);
                serviceNode = code.localVar("node$" + i,
                        code.new_(CMD_ServiceNode_ctor_String_MethodHandle_ServiceGraph_int_List,
                                Const.of(node.name()), mh, graph, Const.of(node.dependentCount()), depList));
            }

            nodeHandles[i] = serviceNode;

            if (i == plan.topIndex()) {
                code.invokeVirtual(MD_ServiceGraph_setTop, graph, serviceNode);
            } else if (i == plan.bottomIndex()) {
                code.invokeVirtual(MD_ServiceGraph_setBottom, graph, serviceNode);
            }
        }

        code.invokeVirtual(MD_ServiceGraph_start, graph);

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
    private LocalVar emitActionHandle(
            BlockCreator code,
            ClassCreator file,
            ServiceGraphBuilder.NodeDescriptor node,
            Map<TransliteratedAction, ConsolidatedRef> consolidatedRefs,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
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
                methodName = phaseName + "$sentinel$" + nodeIndex;
                targetClass = Application.APP_CLASS_NAME;
                boolean isTop = (nodeIndex == 0);
                generateSentinelMethod(file, methodName, isTop);
            }
            case LEGACY_RECORDER -> {
                methodName = phaseName + "$legacy$" + nodeIndex;
                targetClass = Application.APP_CLASS_NAME;
                boolean staticInit = phaseName.equals("static");
                generateLegacyWrapperMethod(file, methodName, node.recorders(),
                        staticInit, generatedClass, substitutions,
                        recordableConstructorBuildItems, loaders, constants);
            }
            case SERVICE, ALIAS, RV_WRAPPER -> {
                ConsolidatedRef ref = consolidatedRefs.get(node.action());
                methodName = "deploy$" + ref.deployIndex();
                targetClass = ref.className();
            }
            case CROSS_PHASE_PROXY -> {
                methodName = phaseName + "$crossphase$" + nodeIndex;
                targetClass = Application.APP_CLASS_NAME;
                generateCrossPhaseProxyMethod(file, methodName, node.name());
            }
            default -> throw new IllegalStateException("Unknown node kind: " + node.kind());
        }

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
    private static LocalVar emitFindStatic(BlockCreator code, String className, String methodName) {
        Expr lookup = code.invokeStatic(MethodDesc.of(MethodHandles.class, "lookup", MethodHandles.Lookup.class));
        Expr targetClass = Const.of(ClassDesc.of(className));
        Expr methodType = code.invokeStatic(
                MethodDesc.of(MethodType.class, "methodType", MethodType.class, Class.class, Class.class),
                Const.of(ConstantDescs.CD_void),
                Const.of(CD_ServiceNode));
        Expr mh = code.invokeVirtual(
                MethodDesc.of(MethodHandles.Lookup.class, "findStatic", MethodHandle.class, Class.class, String.class,
                        MethodType.class),
                lookup, targetClass, Const.of(methodName), methodType);
        return code.localVar("mh$" + className.replace('.', '_').replace('/', '_') + "$" + methodName, mh);
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
        classCreator.staticMethod(methodName, MethodTypeDesc.of(ConstantDescs.CD_void, CD_ServiceNode), mc -> {
            mc.private_();
            ParamVar node = mc.parameter("node");
            mc.body(b0 -> {
                if (isTop) {
                    Expr graph = b0.invokeVirtual(MD_ServiceNode_graph, node);
                    Expr stopRunnable = b0.lambda(Runnable.class, lc -> {
                        Var capturedGraph = lc.capture("graph", graph);
                        lc.body(b1 -> {
                            b1.invokeVirtual(MD_ServiceGraph_signalStopDone, capturedGraph);
                            b1.return_();
                        });
                    });
                    b0.invokeInterface(MethodDesc.of(StartContext.class, "onStop", void.class, Runnable.class),
                            node, stopRunnable);
                } else {
                    Expr graph = b0.invokeVirtual(MD_ServiceNode_graph, node);
                    b0.invokeVirtual(MD_ServiceGraph_signalStartDone, graph);
                }
                b0.invokeVirtual(MD_ServiceNode_startComplete, node);
                b0.return_();
            });
        });
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
     * @param staticInit whether this targets the static-init phase
     * @param generatedClass the producer for generated class build items
     * @param substitutions recorder substitutions
     * @param recordableConstructorBuildItems recorder constructable items
     * @param loaders recorder object loaders
     * @param constants recorder constant definitions
     */
    private void generateLegacyWrapperMethod(
            ClassCreator classCreator,
            String methodName,
            List<BytecodeRecorderImpl> recorders,
            boolean staticInit,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants) {

        classCreator.staticMethod(methodName, MethodTypeDesc.of(ConstantDescs.CD_void, CD_ServiceNode), mc -> {
            mc.private_();
            ParamVar node = mc.parameter("node");
            mc.body(b0 -> {
                LocalVar graph = b0.localVar("graph", b0.invokeVirtual(MD_ServiceNode_graph, node));
                LocalVar startupContext = b0.localVar("startupContext",
                        b0.invokeVirtual(MethodDesc.of(ServiceGraph.class, "startupContext", StartupContext.class), graph));

                LocalVar shutdownContext = b0.localVar("shutdownContext",
                        b0.new_(CMD_NodeShutdownContext_ctor_boolean, Const.of(staticInit)));
                b0.invokeInterface(MethodDesc.of(StartContext.class, "onStop", void.class, Runnable.class),
                        node, shutdownContext);

                b0.invokeVirtual(MD_StartupContext_putValue, startupContext, Const.of(ShutdownContext.class.getName()),
                        shutdownContext);

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
                    // Adapt Gizmo 2 ClassOutput to Gizmo 1 ClassOutput
                    GeneratedClassGizmoAdaptor adaptor = new GeneratedClassGizmoAdaptor(generatedClass, true);
                    recorder.writeBytecode(adaptor::write);

                    LocalVar task = b0.localVar("task", b0.new_(ConstructorDesc.of(ClassDesc.of(recorder.getClassName()))));
                    b0.invokeInterface(
                            MethodDesc.of(StartupTask.class, "deploy", void.class, StartupContext.class),
                            task, startupContext);
                }

                b0.invokeVirtual(MD_ServiceNode_startComplete, node);
                b0.return_();
            });
        });
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
        classCreator.staticMethod(methodName, MethodTypeDesc.of(ConstantDescs.CD_void, CD_ServiceNode), mc -> {
            mc.private_();
            ParamVar node = mc.parameter("node");
            mc.body(b0 -> {
                LocalVar graph = b0.localVar("graph", b0.invokeVirtual(MD_ServiceNode_graph, node));
                LocalVar startupContext = b0.localVar("startupContext",
                        b0.invokeVirtual(MethodDesc.of(ServiceGraph.class, "startupContext", StartupContext.class), graph));

                LocalVar value = b0.localVar("value",
                        b0.invokeVirtual(MD_StartupContext_getServiceValue, startupContext, Const.of(serviceKey)));

                b0.ifElse(b0.eq(value, Const.ofNull(Object.class)),
                        b1 -> b1.invokeVirtual(MD_ServiceNode_startComplete, node),
                        b1 -> b1.invokeVirtual(MD_ServiceNode_startComplete_Object, node, value));
                b0.return_();
            });
        });
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
            boolean hasStaticWithArgs = false;
            boolean hasStaticWithoutArgs = false;
            boolean hasInstanceWithArgs = false;
            boolean hasInstanceWithoutArgs = false;

            MethodInfo withArgs = currentClassInfo.method("main", STRING_ARRAY);
            MethodInfo withoutArgs = currentClassInfo.method("main");

            if (withArgs != null) {
                if (Modifier.isStatic(withArgs.flags())) {
                    if (isTopLevel) {
                        hasStaticWithArgs = true;
                    }
                } else {
                    hasInstanceWithArgs = true;
                }
            }
            if (withoutArgs != null) {
                if (Modifier.isStatic(withoutArgs.flags())) {
                    if (isTopLevel) {
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
                    transformer.modifyMethod(MethodDescriptor.of(withArgs)).rename("$$main$$");
                    result = Result.invalid(transformer.applyTo(classVisitor));
                } else {
                    // here we need to construct an instance and call the instance method with the args parameter
                    MethodCreator standardMain = createStandardMain(transformer);
                    ResultHandle instanceHandle = standardMain
                            .newInstance(MethodDescriptor.ofConstructor(originalMainClassName));
                    ResultHandle argsParamHandle = standardMain.getMethodParam(0);
                    if (isTopLevel) {
                        // we need to rename the method in order to avoid having two main methods with the same name
                        standardMain.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(originalMainClassName, "$$main$$", void.class,
                                        String[].class),
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
                    ResultHandle instanceHandle = standardMain
                            .newInstance(MethodDescriptor.ofConstructor(originalMainClassName));
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
