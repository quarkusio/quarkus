package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageInfo;

import io.quarkus.builder.Version;
import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.ClassOutputBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.SslTrustStoreSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;
import io.quarkus.runtime.Timing;

class MainClassBuildStep {

    private static final String APP_CLASS = "io.quarkus.runner.ApplicationImpl";
    private static final String MAIN_CLASS = "io.quarkus.runner.GeneratedMain";
    private static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";
    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    private static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";

    private static final AtomicInteger COUNT = new AtomicInteger();

    @BuildStep
    MainClassBuildItem build(List<StaticBytecodeRecorderBuildItem> staticInitTasks,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<MainBytecodeRecorderBuildItem> mainMethod,
            List<SystemPropertyBuildItem> properties,
            List<JavaLibraryPathAdditionalPathBuildItem> javaLibraryPathAdditionalPaths,
            Optional<SslTrustStoreSystemPropertyBuildItem> sslTrustStoreSystemProperty,
            List<FeatureBuildItem> features,
            BuildProducer<ApplicationClassNameBuildItem> appClassNameProducer,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            ClassOutputBuildItem classOutput) {

        String appClassName = APP_CLASS + COUNT.incrementAndGet();
        appClassNameProducer.produce(new ApplicationClassNameBuildItem(appClassName));

        // Application class
        ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(classOutput.getClassOutput(), true), appClassName, null,
                Application.class.getName());

        // Application class: static init

        FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT, StartupContext.class);
        scField.setModifiers(Modifier.STATIC);

        MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        //very first thing is to set system props (for build time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
        }

        mv.invokeStaticMethod(MethodDescriptor.ofMethod(Timing.class, "staticInitStarted", void.class));
        ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
        mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
        TryBlock tryBlock = mv.tryBlock();
        for (StaticBytecodeRecorderBuildItem holder : staticInitTasks) {
            final BytecodeRecorderImpl recorder = holder.getBytecodeRecorder();
            if (!recorder.isEmpty()) {
                // Register substitutions in all recorders
                for (ObjectSubstitutionBuildItem sub : substitutions) {
                    ObjectSubstitutionBuildItem.Holder holder1 = sub.holder;
                    recorder.registerSubstitution(holder1.from, holder1.to, holder1.substitution);
                }
                for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                    recorder.registerObjectLoader(item.getObjectLoader());
                }
                recorder.writeBytecode(classOutput.getClassOutput());

                ResultHandle dup = tryBlock.newInstance(ofConstructor(recorder.getClassName()));
                tryBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup,
                        startupContext);
            }
        }
        tryBlock.returnValue(null);

        CatchBlockCreator cb = tryBlock.addCatch(Throwable.class);
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

        tryBlock = mv.tryBlock();

        // Load the run time configuration
        tryBlock.invokeStaticMethod(ConfigurationSetup.CREATE_RUN_TIME_CONFIG);

        for (MainBytecodeRecorderBuildItem holder : mainMethod) {
            final BytecodeRecorderImpl recorder = holder.getBytecodeRecorder();
            if (!recorder.isEmpty()) {
                // Register substitutions in all recorders
                for (ObjectSubstitutionBuildItem sub : substitutions) {
                    ObjectSubstitutionBuildItem.Holder holder1 = sub.holder;
                    recorder.registerSubstitution(holder1.from, holder1.to, holder1.substitution);
                }
                for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                    recorder.registerObjectLoader(item.getObjectLoader());
                }
                recorder.writeBytecode(classOutput.getClassOutput());
                ResultHandle dup = tryBlock.newInstance(ofConstructor(recorder.getClassName()));
                tryBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup,
                        startupContext);
            }
        }

        // Startup log messages
        ResultHandle featuresHandle = tryBlock.load(features.stream()
                .map(f -> f.getInfo())
                .sorted()
                .collect(Collectors.joining(", ")));
        tryBlock.invokeStaticMethod(
                ofMethod(Timing.class, "printStartupTime", void.class, String.class, String.class),
                tryBlock.load(Version.getVersion()), featuresHandle);

        cb = tryBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cb.getCaughtException());
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());
        mv.returnValue(null);

        // Application class: stop method

        mv = file.getMethodCreator("doStop", void.class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
        startupContext = mv.readStaticField(scField.getFieldDescriptor());
        mv.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        mv.returnValue(null);

        // Finish application class
        file.close();

        // Main class

        file = new ClassCreator(ClassOutput.gizmoAdaptor(classOutput.getClassOutput(), true), MAIN_CLASS, null,
                Object.class.getName());

        mv = file.getMethodCreator("main", void.class, String[].class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        final ResultHandle appClassInstance = mv.newInstance(ofConstructor(appClassName));
        // run the app
        mv.invokeVirtualMethod(ofMethod(Application.class, "run", void.class, String[].class), appClassInstance,
                mv.getMethodParam(0));

        mv.returnValue(null);

        file.close();
        return new MainClassBuildItem(MAIN_CLASS);
    }

}
