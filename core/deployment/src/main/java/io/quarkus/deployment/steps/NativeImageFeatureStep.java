package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.graal.GraalVM;

public class NativeImageFeatureStep {

    public static final String GRAAL_FEATURE = "io.quarkus.runner.Feature";

    private static final MethodDescriptor IMAGE_SINGLETONS_LOOKUP = ofMethod(ImageSingletons.class, "lookup", Object.class,
            Class.class);
    private static final MethodDescriptor BUILD_TIME_INITIALIZATION = ofMethod(RuntimeClassInitialization.class,
            "initializeAtBuildTime", void.class, String[].class);
    private static final MethodDescriptor INITIALIZE_CLASSES_AT_RUN_TIME = ofMethod(RuntimeClassInitialization.class,
            "initializeAtRunTime", void.class, Class[].class);
    private static final MethodDescriptor INITIALIZE_PACKAGES_AT_RUN_TIME = ofMethod(RuntimeClassInitialization.class,
            "initializeAtRunTime", void.class, String[].class);
    public static final String RUNTIME_CLASS_INITIALIZATION_SUPPORT = "org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport";
    private static final MethodDescriptor RERUN_INITIALIZATION = ofMethod(
            RUNTIME_CLASS_INITIALIZATION_SUPPORT,
            "rerunInitialization", void.class, Class.class, String.class);

    static final String BEFORE_ANALYSIS_ACCESS = Feature.BeforeAnalysisAccess.class.getName();

    @BuildStep
    void addExportsToNativeImage(BuildProducer<JPMSExportBuildItem> features) {
        // required in order to access org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport
        // prior to 23.1 the class was provided by org.graalvm.sdk module and with 23.1 onwards, it's provided by org.graalvm.nativeimage instead
        features.produce(new JPMSExportBuildItem("org.graalvm.sdk", "org.graalvm.nativeimage.impl", null,
                GraalVM.Version.VERSION_23_1_0));
        features.produce(new JPMSExportBuildItem("org.graalvm.nativeimage", "org.graalvm.nativeimage.impl",
                GraalVM.Version.VERSION_23_1_0));
    }

    @BuildStep
    void generateFeature(BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            BuildProducer<JPMSExportBuildItem> exports,
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItems,
            List<RuntimeInitializedPackageBuildItem> runtimeInitializedPackageBuildItems,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClassBuildItems,
            List<UnsafeAccessedFieldBuildItem> unsafeAccessedFields) {
        ClassCreator file = new ClassCreator(new ClassOutput() {
            @Override
            public void write(String s, byte[] bytes) {
                nativeImageClass.produce(new GeneratedNativeImageClassBuildItem(s, bytes));
            }
        }, GRAAL_FEATURE, null,
                Object.class.getName(), Feature.class.getName());

        // Add getDescription method
        MethodCreator getDescription = file.getMethodCreator("getDescription", String.class);
        getDescription.returnValue(getDescription.load("Auto-generated class by Quarkus from the existing extensions"));

        MethodCreator beforeAn = file.getMethodCreator("beforeAnalysis", "V", BEFORE_ANALYSIS_ACCESS);
        TryBlock overallCatch = beforeAn.tryBlock();

        overallCatch.invokeStaticMethod(BUILD_TIME_INITIALIZATION,
                overallCatch.marshalAsArray(String.class, overallCatch.load(""))); // empty string means initialize everything

        if (!runtimeInitializedClassBuildItems.isEmpty()) {
            //  Class[] runtimeInitializedClasses()
            MethodCreator runtimeInitializedClasses = file
                    .getMethodCreator("runtimeInitializedClasses", Class[].class)
                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC);

            ResultHandle thisClass = runtimeInitializedClasses.loadClassFromTCCL(GRAAL_FEATURE);
            ResultHandle cl = runtimeInitializedClasses.invokeVirtualMethod(
                    ofMethod(Class.class, "getClassLoader", ClassLoader.class),
                    thisClass);
            ResultHandle classesArray = runtimeInitializedClasses.newArray(Class.class,
                    runtimeInitializedClasses.load(runtimeInitializedClassBuildItems.size()));
            for (int i = 0; i < runtimeInitializedClassBuildItems.size(); i++) {
                TryBlock tc = runtimeInitializedClasses.tryBlock();
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(runtimeInitializedClassBuildItems.get(i).getClassName()), tc.load(false), cl);
                tc.writeArrayValue(classesArray, i, clazz);
                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }
            runtimeInitializedClasses.returnValue(classesArray);

            ResultHandle classes = overallCatch.invokeStaticMethod(runtimeInitializedClasses.getMethodDescriptor());
            overallCatch.invokeStaticMethod(INITIALIZE_CLASSES_AT_RUN_TIME, classes);
        }

        if (!runtimeInitializedPackageBuildItems.isEmpty()) {
            //  String[] runtimeInitializedPackages()
            MethodCreator runtimeInitializedPackages = file
                    .getMethodCreator("runtimeInitializedPackages", String[].class)
                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC);

            ResultHandle packagesArray = runtimeInitializedPackages.newArray(String.class,
                    runtimeInitializedPackages.load(runtimeInitializedPackageBuildItems.size()));
            for (int i = 0; i < runtimeInitializedPackageBuildItems.size(); i++) {
                TryBlock tc = runtimeInitializedPackages.tryBlock();
                ResultHandle pkg = tc.load(runtimeInitializedPackageBuildItems.get(i).getPackageName());
                tc.writeArrayValue(packagesArray, i, pkg);
                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }
            runtimeInitializedPackages.returnValue(packagesArray);

            ResultHandle packages = overallCatch.invokeStaticMethod(runtimeInitializedPackages.getMethodDescriptor());
            overallCatch.invokeStaticMethod(INITIALIZE_PACKAGES_AT_RUN_TIME, packages);
        }

        // hack in reinitialization of process info classes
        if (!runtimeReinitializedClassBuildItems.isEmpty()) {
            MethodCreator runtimeReinitializedClasses = file
                    .getMethodCreator("runtimeReinitializedClasses", void.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC);

            ResultHandle thisClass = runtimeReinitializedClasses.loadClassFromTCCL(GRAAL_FEATURE);
            ResultHandle cl = runtimeReinitializedClasses.invokeVirtualMethod(
                    ofMethod(Class.class, "getClassLoader", ClassLoader.class),
                    thisClass);
            ResultHandle quarkus = runtimeReinitializedClasses.load("Quarkus");
            ResultHandle imageSingleton = runtimeReinitializedClasses.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                    runtimeReinitializedClasses.loadClassFromTCCL(RUNTIME_CLASS_INITIALIZATION_SUPPORT));
            for (RuntimeReinitializedClassBuildItem runtimeReinitializedClass : runtimeReinitializedClassBuildItems) {
                TryBlock tc = runtimeReinitializedClasses.tryBlock();
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(runtimeReinitializedClass.getClassName()), tc.load(false), cl);
                tc.invokeInterfaceMethod(RERUN_INITIALIZATION, imageSingleton, clazz, quarkus);

                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }
            runtimeReinitializedClasses.returnVoid();

            overallCatch.invokeStaticMethod(runtimeReinitializedClasses.getMethodDescriptor());
        }

        // Ensure registration of fields being accessed through unsafe is done last to ensure that the class
        // initialization configuration is done first.  Registering the fields before configuring class initialization
        // may results in classes being marked for runtime initialization even if not explicitly requested.
        if (!unsafeAccessedFields.isEmpty()) {
            ResultHandle beforeAnalysisParam = beforeAn.getMethodParam(0);
            MethodCreator registerAsUnsafeAccessed = file
                    .getMethodCreator("registerAsUnsafeAccessed", void.class, Feature.BeforeAnalysisAccess.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC);
            ResultHandle thisClass = registerAsUnsafeAccessed.loadClassFromTCCL(GRAAL_FEATURE);
            ResultHandle cl = registerAsUnsafeAccessed
                    .invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class), thisClass);
            for (UnsafeAccessedFieldBuildItem unsafeAccessedField : unsafeAccessedFields) {
                TryBlock tc = registerAsUnsafeAccessed.tryBlock();
                ResultHandle declaringClassHandle = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(unsafeAccessedField.getDeclaringClass()), tc.load(false), cl);
                ResultHandle fieldHandle = tc.invokeVirtualMethod(
                        ofMethod(Class.class, "getDeclaredField", Field.class, String.class), declaringClassHandle,
                        tc.load(unsafeAccessedField.getFieldName()));
                tc.invokeInterfaceMethod(
                        ofMethod(Feature.BeforeAnalysisAccess.class, "registerAsUnsafeAccessed", void.class, Field.class),
                        registerAsUnsafeAccessed.getMethodParam(0), fieldHandle);
                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }
            registerAsUnsafeAccessed.returnVoid();
            overallCatch.invokeStaticMethod(registerAsUnsafeAccessed.getMethodDescriptor(), beforeAnalysisParam);
        }

        CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
        print.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), print.getCaughtException());

        beforeAn.loadClassFromTCCL("io.quarkus.runner.ApplicationImpl");
        beforeAn.returnValue(null);

        file.close();
    }

}
