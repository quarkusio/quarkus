package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.logmanager.handlers.DelayedHandler;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.builder.Version;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.SslTrustStoreSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.RunTimeConfigurationGenerator;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.NativeImageRuntimePropertiesRecorder;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.configuration.ProfileManager;

class MainClassBuildStep {

    static final String MAIN_CLASS = "io.quarkus.runner.GeneratedMain";
    static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";
    static final String LOG = "LOG";
    static final String JAVA_LIBRARY_PATH = "java.library.path";
    static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";

    private static final FieldDescriptor STARTUP_CONTEXT_FIELD = FieldDescriptor.of(Application.APP_CLASS_NAME, STARTUP_CONTEXT,
            StartupContext.class);

    @BuildStep
    void build(List<StaticBytecodeRecorderBuildItem> staticInitTasks,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<MainBytecodeRecorderBuildItem> mainMethod,
            List<SystemPropertyBuildItem> properties,
            List<JavaLibraryPathAdditionalPathBuildItem> javaLibraryPathAdditionalPaths,
            Optional<SslTrustStoreSystemPropertyBuildItem> sslTrustStoreSystemProperty,
            List<FeatureBuildItem> features,
            BuildProducer<ApplicationClassNameBuildItem> appClassNameProducer,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReloadBuildItem,
            ApplicationInfoBuildItem applicationInfo) {

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

        MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        //very first thing is to set system props (for build time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
        }
        //set the launch mode
        ResultHandle lm = mv
                .readStaticField(FieldDescriptor.of(LaunchMode.class, launchMode.getLaunchMode().name(), LaunchMode.class));
        mv.invokeStaticMethod(MethodDescriptor.ofMethod(ProfileManager.class, "setLaunchMode", void.class, LaunchMode.class),
                lm);

        mv.invokeStaticMethod(MethodDescriptor.ofMethod(Timing.class, "staticInitStarted", void.class));

        // ensure that the config class is initialized
        mv.invokeStaticMethod(RunTimeConfigurationGenerator.C_ENSURE_INITIALIZED);
        if (liveReloadBuildItem.isLiveReload()) {
            mv.invokeStaticMethod(RunTimeConfigurationGenerator.REINIT);
        }
        // Init the LOG instance
        mv.writeStaticField(logField.getFieldDescriptor(), mv.invokeStaticMethod(
                ofMethod(Logger.class, "getLogger", Logger.class, String.class), mv.load("io.quarkus.application")));

        ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
        mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
        TryBlock tryBlock = mv.tryBlock();
        for (StaticBytecodeRecorderBuildItem holder : staticInitTasks) {
            writeRecordedBytecode(holder.getBytecodeRecorder(), null, substitutions, loaders, gizmoOutput, startupContext,
                    tryBlock);
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

        // very first thing is to set system props (for run time, which use substitutions for a different
        // storage from build-time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
        }
        mv.invokeStaticMethod(ofMethod(NativeImageRuntimePropertiesRecorder.class, "doRuntime", void.class));

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

        if (sslTrustStoreSystemProperty.isPresent()) {
            ResultHandle alreadySetTrustStore = mv.invokeStaticMethod(
                    ofMethod(System.class, "getProperty", String.class, String.class),
                    mv.load(JAVAX_NET_SSL_TRUST_STORE));

            BytecodeCreator inGraalVMCode = mv
                    .ifNonZero(mv.invokeStaticMethod(ofMethod(ImageInfo.class, "inImageRuntimeCode", boolean.class)))
                    .trueBranch();

            inGraalVMCode.ifNull(alreadySetTrustStore).trueBranch().invokeStaticMethod(
                    ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    inGraalVMCode.load(JAVAX_NET_SSL_TRUST_STORE),
                    inGraalVMCode.load(sslTrustStoreSystemProperty.get().getPath()));
        }

        mv.invokeStaticMethod(ofMethod(Timing.class, "mainStarted", void.class));
        startupContext = mv.readStaticField(scField.getFieldDescriptor());

        //now set the command line arguments
        mv.invokeVirtualMethod(
                MethodDescriptor.ofMethod(StartupContext.class, "setCommandLineArguments", void.class, String[].class),
                startupContext, mv.getMethodParam(0));

        tryBlock = mv.tryBlock();

        for (MainBytecodeRecorderBuildItem holder : mainMethod) {
            writeRecordedBytecode(holder.getBytecodeRecorder(), holder.getGeneratedStartupContextClassName(), substitutions,
                    loaders, gizmoOutput, startupContext, tryBlock);
        }

        // Startup log messages
        ResultHandle featuresHandle = tryBlock.load(features.stream()
                .map(f -> f.getInfo())
                .sorted()
                .collect(Collectors.joining(", ")));
        ResultHandle activeProfile = tryBlock
                .invokeStaticMethod(ofMethod(ProfileManager.class, "getActiveProfile", String.class));
        tryBlock.invokeStaticMethod(
                ofMethod(Timing.class, "printStartupTime", void.class, String.class, String.class, String.class, String.class,
                        String.class, boolean.class),
                tryBlock.load(applicationInfo.getName()),
                tryBlock.load(applicationInfo.getVersion()),
                tryBlock.load(Version.getVersion()),
                featuresHandle,
                activeProfile,
                tryBlock.load(LaunchMode.DEVELOPMENT.equals(launchMode.getLaunchMode())));

        cb = tryBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(Logger.class, "error", void.class, Object.class, Throwable.class),
                cb.readStaticField(logField.getFieldDescriptor()), cb.load("Failed to start application"),
                cb.getCaughtException());

        // an exception was thrown before logging was actually setup, we simply dump everything to the console
        ResultHandle delayedHandler = cb
                .readStaticField(FieldDescriptor.of(InitialConfigurator.class, "DELAYED_HANDLER", DelayedHandler.class));
        ResultHandle isActivated = cb.invokeVirtualMethod(ofMethod(DelayedHandler.class, "isActivated", boolean.class),
                delayedHandler);
        BytecodeCreator isActivatedFalse = cb.ifNonZero(isActivated).falseBranch();
        ResultHandle handlersArray = isActivatedFalse.newArray(Handler.class, 1);
        isActivatedFalse.writeArrayValue(handlersArray, 0, isActivatedFalse.newInstance(ofConstructor(ConsoleHandler.class)));
        isActivatedFalse.invokeVirtualMethod(ofMethod(DelayedHandler.class, "setHandlers", Handler[].class, Handler[].class),
                delayedHandler, handlersArray);
        isActivatedFalse.breakScope();

        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());
        mv.returnValue(null);

        // Application class: stop method

        mv = file.getMethodCreator("doStop", void.class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
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
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            PackageConfig packageConfig) {
        String mainClassName = MAIN_CLASS;
        Map<String, String> quarkusMainAnnotations = new HashMap<>();
        Collection<AnnotationInstance> quarkusMains = combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(QuarkusMain.class.getName()));
        for (AnnotationInstance i : quarkusMains) {
            AnnotationValue nameValue = i.value("name");
            String name = "";
            if (nameValue != null) {
                name = nameValue.asString();
            }
            if (quarkusMainAnnotations.containsKey(name)) {
                throw new RuntimeException(
                        "More than one @QuarkusMain method found with name '" + name + "': "
                                + i.target().asClass().name() + " and " + quarkusMainAnnotations.get(name));
            }
            quarkusMainAnnotations.put(name, i.target().asClass().name().toString());
        }

        if (packageConfig.mainClass.isPresent()) {
            String mainAnnotationClass = quarkusMainAnnotations.get(packageConfig.mainClass.get());
            if (mainAnnotationClass != null) {
                mainClassName = mainAnnotationClass;
            } else {
                mainClassName = packageConfig.mainClass.get();
            }
        } else if (quarkusMainAnnotations.containsKey("")) {
            mainClassName = quarkusMainAnnotations.get("");
        }
        if (mainClassName.equals(MAIN_CLASS)) {
            //generate a main that just runs the app, the user has not supplied a main class
            ClassCreator file = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClass, true), MAIN_CLASS, null,
                    Object.class.getName());

            MethodCreator mv = file.getMethodCreator("main", void.class, String[].class);
            mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            mv.invokeStaticMethod(MethodDescriptor.ofMethod(Quarkus.class, "run", void.class, String[].class),
                    mv.getMethodParam(0));
            mv.returnValue(null);

            file.close();
        } else {
            Collection<ClassInfo> impls = combinedIndexBuildItem.getIndex()
                    .getAllKnownImplementors(DotName.createSimple(QuarkusApplication.class.getName()));
            boolean found = false;
            for (ClassInfo i : impls) {
                if (i.name().toString().equals(mainClassName)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                //this is QuarkusApplication, generate a real main to run it
                ClassCreator file = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClass, true), MAIN_CLASS, null,
                        Object.class.getName());

                MethodCreator mv = file.getMethodCreator("main", void.class, String[].class);
                mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
                mv.invokeStaticMethod(MethodDescriptor.ofMethod(Quarkus.class, "run", void.class, Class.class, String[].class),
                        mv.loadClass(mainClassName),
                        mv.getMethodParam(0));
                mv.returnValue(null);
                file.close();
                mainClassName = MAIN_CLASS;
            }
        }

        return new MainClassBuildItem(mainClassName);
    }

    private void writeRecordedBytecode(BytecodeRecorderImpl recorder, String fallbackGeneratedStartupTaskClassName,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders, GeneratedClassGizmoAdaptor gizmoOutput,
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
            recorder.writeBytecode(gizmoOutput);
        }

        ResultHandle dup = bytecodeCreator
                .newInstance(ofConstructor(recorder != null ? recorder.getClassName() : fallbackGeneratedStartupTaskClassName));
        bytecodeCreator.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup,
                startupContext);
    }

    /**
     * registers the generated application class for reflection, needed when launching via the Quarkus launcher
     *
     */
    @BuildStep
    ReflectiveClassBuildItem applicationReflection() {
        return new ReflectiveClassBuildItem(false, false, Application.APP_CLASS_NAME);
    }

}
