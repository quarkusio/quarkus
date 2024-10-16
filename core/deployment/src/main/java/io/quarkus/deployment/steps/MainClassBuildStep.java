package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.KotlinUtil.isKotlinClass;
import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.quarkus.builder.Version;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AllowJNDIBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderConstantDefinitionBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.deployment.builditem.RecordableConstructorBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.naming.NamingConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSControlPointBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSRequestedBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.dev.console.QuarkusConsole;
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
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.ExecutionModeManager;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.NativeImageRuntimePropertiesRecorder;
import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.appcds.AppCDSUtil;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.util.StepTiming;

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
    void build(List<StaticBytecodeRecorderBuildItem> staticInitTasks,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<MainBytecodeRecorderBuildItem> mainMethod,
            List<SystemPropertyBuildItem> properties,
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
            Optional<AppCDSRequestedBuildItem> appCDSRequested,
            Optional<AppCDSControlPointBuildItem> appCDSControlPoint,
            NamingConfig namingConfig) {

        appClassNameProducer.produce(new ApplicationClassNameBuildItem(Application.APP_CLASS_NAME));

        // Application class
        GeneratedClassGizmoAdaptor gizmoOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        ClassCreator file = new ClassCreator(gizmoOutput, Application.APP_CLASS_NAME, null,
                Application.class.getName());

        // Application class: static init

        // LOG static field
        FieldCreator logField = file.getFieldCreator(LOG, Logger.class).setModifiers(Modifier.STATIC);

        FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT_FIELD);
        scField.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        FieldCreator quarkusVersionField = file.getFieldCreator(QUARKUS_ANALYTICS_QUARKUS_VERSION, String.class)
                .setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);

        MethodCreator ctor = file.getMethodCreator("<init>", void.class);
        ctor.invokeSpecialMethod(ofMethod(Application.class, "<init>", void.class, boolean.class),
                ctor.getThis(), ctor.load(launchMode.isAuxiliaryApplication()));
        ctor.returnValue(null);

        MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        if (!namingConfig.enableJndi && allowJNDIBuildItems.isEmpty()) {
            mv.invokeStaticMethod(ofMethod(DisabledInitialContextManager.class, "register", void.class));
        }

        //very first thing is to set system props (for build time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
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

        // ensure that the config class is initialized
        mv.invokeStaticMethod(RunTimeConfigurationGenerator.C_ENSURE_INITIALIZED);
        if (liveReloadBuildItem.isLiveReload()) {
            mv.invokeStaticMethod(RunTimeConfigurationGenerator.REINIT);
        }
        // Init the LOG instance
        mv.writeStaticField(logField.getFieldDescriptor(), mv.invokeStaticMethod(
                ofMethod(Logger.class, "getLogger", Logger.class, String.class), mv.load("io.quarkus.application")));

        // Init the __QUARKUS_ANALYTICS_QUARKUS_VERSION field
        mv.writeStaticField(quarkusVersionField.getFieldDescriptor(),
                mv.load("__quarkus_analytics__quarkus.version=" + Version.getVersion()));

        ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
        mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
        TryBlock tryBlock = mv.tryBlock();
        tryBlock.invokeStaticMethod(CONFIGURE_STEP_TIME_START);
        for (StaticBytecodeRecorderBuildItem holder : staticInitTasks) {
            writeRecordedBytecode(holder.getBytecodeRecorder(), null, substitutions, recordableConstructorBuildItems, loaders,
                    constants, gizmoOutput, startupContext, tryBlock);
        }
        tryBlock.returnValue(null);

        CatchBlockCreator cb = tryBlock.addCatch(Throwable.class);
        cb.invokeStaticMethod(ofMethod(ApplicationStateNotification.class, "notifyStartupFailed", void.class, Throwable.class),
                cb.getCaughtException());
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());

        // Application class: start method

        mv = file.getMethodCreator("doStart", void.class, String[].class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);

        // if AppCDS generation was requested and no other code has requested handling of the process,
        // we ensure that the application simply loads some classes from a file and terminates
        if (appCDSRequested.isPresent() && appCDSControlPoint.isEmpty()) {
            ResultHandle createAppCDsSysProp = mv.invokeStaticMethod(
                    ofMethod(System.class, "getProperty", String.class, String.class, String.class),
                    mv.load(GENERATE_APP_CDS_SYSTEM_PROPERTY), mv.load("false"));
            ResultHandle createAppCDSBool = mv.invokeStaticMethod(
                    ofMethod(Boolean.class, "parseBoolean", boolean.class, String.class), createAppCDsSysProp);
            BytecodeCreator createAppCDS = mv.ifTrue(createAppCDSBool).trueBranch();

            createAppCDS.invokeStaticMethod(ofMethod(AppCDSUtil.class, "loadGeneratedClasses", void.class));

            createAppCDS.invokeStaticMethod(ofMethod(ApplicationLifecycleManager.class, "exit", void.class));
            createAppCDS.returnValue(null);
        }

        // very first thing is to set system props (for run time, which use substitutions for a different
        // storage from build-time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
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
        startupContext = mv.readStaticField(scField.getFieldDescriptor());

        //now set the command line arguments
        mv.invokeVirtualMethod(
                ofMethod(StartupContext.class, "setCommandLineArguments", void.class, String[].class),
                startupContext, mv.getMethodParam(0));

        mv.invokeStaticMethod(CONFIGURE_STEP_TIME_ENABLED);

        tryBlock = mv.tryBlock();
        tryBlock.invokeStaticMethod(CONFIGURE_STEP_TIME_START);
        for (MainBytecodeRecorderBuildItem holder : mainMethod) {
            writeRecordedBytecode(holder.getBytecodeRecorder(), holder.getGeneratedStartupContextClassName(), substitutions,
                    recordableConstructorBuildItems,
                    loaders, constants, gizmoOutput, startupContext, tryBlock);
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

        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());
        mv.returnValue(null);

        // Application class: stop method

        mv = file.getMethodCreator("doStop", void.class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
        mv.invokeStaticMethod(RUNTIME_EXECUTION_UNSET);
        startupContext = mv.readStaticField(scField.getFieldDescriptor());
        mv.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
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
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
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

    private void writeRecordedBytecode(BytecodeRecorderImpl recorder, String fallbackGeneratedStartupTaskClassName,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<RecordableConstructorBuildItem> recordableConstructorBuildItems,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            List<BytecodeRecorderConstantDefinitionBuildItem> constants,
            GeneratedClassGizmoAdaptor gizmoOutput,
            ResultHandle startupContext, BytecodeCreator bytecodeCreator) {

        if ((recorder == null || recorder.isEmpty()) && fallbackGeneratedStartupTaskClassName == null) {
            return;
        }

        if ((recorder != null) && !recorder.isEmpty()) {
            for (ObjectSubstitutionBuildItem sub : substitutions) {
                ObjectSubstitutionBuildItem.Holder holder1 = sub.holder;
                recorder.registerSubstitution(holder1.from, holder1.to, holder1.substitution);
            }
            for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                recorder.registerObjectLoader(item.getObjectLoader());
            }
            for (var item : recordableConstructorBuildItems) {
                recorder.markClassAsConstructorRecordable(item.getClazz());
            }
            for (BytecodeRecorderConstantDefinitionBuildItem constant : constants) {
                constant.register(recorder);
            }
            recorder.writeBytecode(gizmoOutput);
        }

        ResultHandle dup = bytecodeCreator
                .newInstance(ofConstructor(recorder != null ? recorder.getClassName() : fallbackGeneratedStartupTaskClassName));
        bytecodeCreator.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup,
                startupContext);
        bytecodeCreator.invokeStaticMethod(PRINT_STEP_TIME_METHOD, startupContext);
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
