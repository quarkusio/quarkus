package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.graalvm.home.Version;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.deployment.pkg.steps.GraalVM;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.NativeImageFeatureUtils;
import io.quarkus.runtime.ResourceHelper;
import io.quarkus.runtime.graal.ResourcesFeature;

public class NativeImageFeatureStep {

    public static final String GRAAL_FEATURE = "io.quarkus.runner.Feature";
    private static final MethodDescriptor VERSION_CURRENT = ofMethod(Version.class, "getCurrent", Version.class);
    private static final MethodDescriptor VERSION_COMPARE_TO = ofMethod(Version.class, "compareTo", int.class, int[].class);

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

    public static final String CONFIGURATION_CONDITION = "org.graalvm.nativeimage.impl.ConfigurationCondition";
    private static final MethodDescriptor CONFIGURATION_ALWAYS_TRUE = ofMethod(
            CONFIGURATION_CONDITION,
            "alwaysTrue", CONFIGURATION_CONDITION);

    private static final MethodDescriptor LOOKUP_METHOD = ofMethod(
            NativeImageFeatureUtils.class,
            "lookupMethod", Method.class, Class.class, String.class, Class[].class);

    private static final MethodDescriptor FIND_MODULE_METHOD = ofMethod(
            NativeImageFeatureUtils.class,
            "findModule", Module.class, String.class);
    private static final MethodDescriptor INVOKE = ofMethod(
            Method.class, "invoke", Object.class, Object.class, Object[].class);
    static final String BEFORE_ANALYSIS_ACCESS = Feature.BeforeAnalysisAccess.class.getName();
    static final String LOCALIZATION_FEATURE = "com.oracle.svm.core.jdk.localization.LocalizationFeature";
    static final String RUNTIME_RESOURCE_SUPPORT = "org.graalvm.nativeimage.impl.RuntimeResourceSupport";

    @BuildStep
    GeneratedResourceBuildItem generateNativeResourcesList(List<NativeImageResourceBuildItem> resources,
            BuildProducer<NativeImageResourcePatternsBuildItem> resourcePatternsBuildItemBuildProducer) {
        StringBuilder sb = new StringBuilder();
        for (NativeImageResourceBuildItem i : resources) {
            for (String r : i.getResources()) {
                sb.append(r);
                sb.append("\n");
            }
        }
        //we don't want this file in the final image
        resourcePatternsBuildItemBuildProducer.produce(NativeImageResourcePatternsBuildItem.builder()
                .excludePattern(ResourcesFeature.META_INF_QUARKUS_NATIVE_RESOURCES_TXT).build());
        return new GeneratedResourceBuildItem(ResourcesFeature.META_INF_QUARKUS_NATIVE_RESOURCES_TXT,
                sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @BuildStep
    void addExportsToNativeImage(BuildProducer<JPMSExportBuildItem> features,
            List<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses) {
        // required in order to access org.graalvm.nativeimage.impl.RuntimeSerializationSupport and org.graalvm.nativeimage.impl.ConfigurationCondition
        features.produce(new JPMSExportBuildItem("org.graalvm.sdk", "org.graalvm.nativeimage.impl"));
        // required in order to access com.oracle.svm.core.jni.JNIRuntimeAccess in GraalVM 22.2.x
        if (jniRuntimeAccessibleClasses != null && !jniRuntimeAccessibleClasses.isEmpty()) {
            features.produce(new JPMSExportBuildItem("org.graalvm.nativeimage.builder", "com.oracle.svm.core.jni",
                    null, GraalVM.Version.VERSION_22_3_0));
        }
    }

    @BuildStep
    void generateFeature(BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            BuildProducer<JPMSExportBuildItem> exports,
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItems,
            List<RuntimeInitializedPackageBuildItem> runtimeInitializedPackageBuildItems,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClassBuildItems,
            List<NativeImageResourcePatternsBuildItem> resourcePatterns,
            List<NativeImageResourceBundleBuildItem> resourceBundles,
            List<ServiceProviderBuildItem> serviceProviderBuildItems,
            List<UnsafeAccessedFieldBuildItem> unsafeAccessedFields) {
        ClassCreator file = new ClassCreator(new ClassOutput() {
            @Override
            public void write(String s, byte[] bytes) {
                nativeImageClass.produce(new GeneratedNativeImageClassBuildItem(s, bytes));
            }
        }, GRAAL_FEATURE, null,
                Object.class.getName(), Feature.class.getName());

        // Add getDescription (from GraalVM 22.2.0+)
        MethodCreator getDescription = file.getMethodCreator("getDescription", String.class);
        getDescription.returnValue(getDescription.load("Auto-generated class by Quarkus from the existing extensions"));

        MethodCreator beforeAn = file.getMethodCreator("beforeAnalysis", "V", BEFORE_ANALYSIS_ACCESS);
        TryBlock overallCatch = beforeAn.tryBlock();

        ResultHandle beforeAnalysisParam = beforeAn.getMethodParam(0);

        MethodCreator registerAsUnsafeAccessed = file
                .getMethodCreator("registerAsUnsafeAccessed", void.class, Feature.BeforeAnalysisAccess.class)
                .setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        for (UnsafeAccessedFieldBuildItem unsafeAccessedField : unsafeAccessedFields) {
            TryBlock tc = registerAsUnsafeAccessed.tryBlock();
            ResultHandle declaringClassHandle = tc.invokeStaticMethod(
                    ofMethod(Class.class, "forName", Class.class, String.class),
                    tc.load(unsafeAccessedField.getDeclaringClass()));
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

        /* Resource includes and excludes */
        if (!resourcePatterns.isEmpty()) {
            MethodCreator resourceIncludesExcludes = file
                    .getMethodCreator("resourceIncludesExcludes", void.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC);

            // Needed to access com.oracle.svm.core.configure.ResourcesRegistry.* in GraalVM 22.2
            exports.produce(new JPMSExportBuildItem("org.graalvm.nativeimage.builder", "com.oracle.svm.core.configure",
                    null, GraalVM.Version.VERSION_22_3_0));

            TryBlock tc = resourceIncludesExcludes.tryBlock();

            ResultHandle resourcesArgTypes = tc.marshalAsArray(Class.class, tc.loadClassFromTCCL(CONFIGURATION_CONDITION),
                    tc.loadClassFromTCCL(String.class));
            AssignableResultHandle resourcesArgs = tc.createVariable(Object[].class);
            tc.assign(resourcesArgs,
                    tc.marshalAsArray(Object.class, tc.invokeStaticMethod(CONFIGURATION_ALWAYS_TRUE), tc.loadNull()));

            AssignableResultHandle ignoreResourcesMethod = tc.createVariable(Method.class);
            AssignableResultHandle addResourcesMethod = tc.createVariable(Method.class);
            AssignableResultHandle resourcesSingleton = tc.createVariable(Object.class);

            BranchResult graalVm22_3Test = tc.ifGreaterEqualZero(tc.invokeVirtualMethod(VERSION_COMPARE_TO,
                    tc.invokeStaticMethod(VERSION_CURRENT),
                    tc.marshalAsArray(int.class, tc.load(22), tc.load(3))));
            /* GraalVM >= 22.3 */
            try (BytecodeCreator greaterThan22_2 = graalVm22_3Test.trueBranch()) {

                ResultHandle runtimeResourceSupportClass = greaterThan22_2.loadClassFromTCCL(RUNTIME_RESOURCE_SUPPORT);

                greaterThan22_2.assign(resourcesSingleton, greaterThan22_2.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                        runtimeResourceSupportClass));

                greaterThan22_2.assign(ignoreResourcesMethod, greaterThan22_2.invokeStaticMethod(LOOKUP_METHOD,
                        runtimeResourceSupportClass, greaterThan22_2.load("ignoreResources"), resourcesArgTypes));
                greaterThan22_2.assign(addResourcesMethod, greaterThan22_2.invokeStaticMethod(LOOKUP_METHOD,
                        runtimeResourceSupportClass, greaterThan22_2.load("addResources"), resourcesArgTypes));
            }

            /* GraalVM < 22.3 */
            try (BytecodeCreator smallerThan22_3 = graalVm22_3Test.falseBranch()) {

                ResultHandle resourceRegistryClass = smallerThan22_3
                        .loadClassFromTCCL("com.oracle.svm.core.configure.ResourcesRegistry");
                smallerThan22_3.assign(resourcesSingleton, smallerThan22_3.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                        resourceRegistryClass));

                smallerThan22_3.assign(ignoreResourcesMethod, smallerThan22_3.invokeStaticMethod(LOOKUP_METHOD,
                        resourceRegistryClass, smallerThan22_3.load("ignoreResources"), resourcesArgTypes));
                smallerThan22_3.assign(addResourcesMethod, smallerThan22_3.invokeStaticMethod(LOOKUP_METHOD,
                        resourceRegistryClass, smallerThan22_3.load("addResources"), resourcesArgTypes));
            }

            ResultHandle indexOne = tc.load(1);

            for (NativeImageResourcePatternsBuildItem resourcePatternsItem : resourcePatterns) {
                for (String pattern : resourcePatternsItem.getExcludePatterns()) {
                    tc.writeArrayValue(resourcesArgs, indexOne, tc.load(pattern));
                    tc.invokeVirtualMethod(INVOKE, ignoreResourcesMethod, resourcesSingleton, resourcesArgs);
                }
                for (String pattern : resourcePatternsItem.getIncludePatterns()) {
                    tc.writeArrayValue(resourcesArgs, indexOne, tc.load(pattern));
                    tc.invokeVirtualMethod(INVOKE, addResourcesMethod, resourcesSingleton, resourcesArgs);
                }
            }
            CatchBlockCreator cc = tc.addCatch(Throwable.class);
            cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());

            resourceIncludesExcludes.returnVoid();
            overallCatch.invokeStaticMethod(resourceIncludesExcludes.getMethodDescriptor());
        }

        MethodCreator registerServiceProviders = file
                .getMethodCreator("registerServiceProviders", void.class)
                .setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            registerServiceProviders.invokeStaticMethod(
                    ofMethod(ResourceHelper.class, "registerResources", void.class, String.class),
                    registerServiceProviders.load(i.serviceDescriptorFile()));
        }
        registerServiceProviders.returnVoid();
        overallCatch.invokeStaticMethod(registerServiceProviders.getMethodDescriptor());

        if (!resourceBundles.isEmpty()) {
            MethodCreator registerResourceBundles = file
                    .getMethodCreator("registerResourceBundles", void.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC);

            // Needed to access LOCALIZATION_FEATURE
            exports.produce(
                    new JPMSExportBuildItem("org.graalvm.nativeimage.builder", "com.oracle.svm.core.jdk.localization",
                            null, GraalVM.Version.VERSION_22_3_0));

            BranchResult graalVm22_3Test = registerResourceBundles
                    .ifGreaterEqualZero(registerResourceBundles.invokeVirtualMethod(VERSION_COMPARE_TO,
                            registerResourceBundles.invokeStaticMethod(VERSION_CURRENT),
                            registerResourceBundles.marshalAsArray(int.class, registerResourceBundles.load(22),
                                    registerResourceBundles.load(3))));
            /* GraalVM >= 22.3 */
            try (BytecodeCreator greaterThan22_2 = graalVm22_3Test.trueBranch()) {

                MethodDescriptor addResourceBundle = ofMethod("org.graalvm.nativeimage.hosted.RuntimeResourceAccess",
                        "addResourceBundle", void.class, Module.class, String.class);

                for (NativeImageResourceBundleBuildItem i : resourceBundles) {
                    TryBlock tc = greaterThan22_2.tryBlock();

                    String moduleName = i.getModuleName();
                    ResultHandle moduleNameHandle;
                    if (moduleName == null) {
                        moduleNameHandle = tc.loadNull();
                    } else {
                        moduleNameHandle = tc.load(moduleName);
                    }
                    ResultHandle module = tc.invokeStaticMethod(FIND_MODULE_METHOD, moduleNameHandle);
                    tc.invokeStaticMethod(addResourceBundle, module, tc.load(i.getBundleName()));
                    CatchBlockCreator c = tc.addCatch(Throwable.class);
                    //c.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), c.getCaughtException());
                }
            }

            /* GraalVM < 22.3 */
            try (BytecodeCreator smallerThan22_3 = graalVm22_3Test.falseBranch()) {

                ResultHandle locClass = smallerThan22_3.loadClassFromTCCL(LOCALIZATION_FEATURE);
                ResultHandle newParams = smallerThan22_3.marshalAsArray(Class.class,
                        smallerThan22_3.loadClassFromTCCL(String.class));
                ResultHandle registerMethod = smallerThan22_3.invokeStaticMethod(
                        LOOKUP_METHOD,
                        locClass, smallerThan22_3.load("prepareBundle"), newParams);

                ResultHandle locSupport = smallerThan22_3.invokeStaticMethod(
                        IMAGE_SINGLETONS_LOOKUP,
                        locClass);

                for (NativeImageResourceBundleBuildItem i : resourceBundles) {
                    TryBlock et = smallerThan22_3.tryBlock();

                    et.invokeVirtualMethod(ofMethod(Method.class, "invoke", Object.class, Object.class, Object[].class),
                            registerMethod, locSupport, et.marshalAsArray(Object.class, et.load(i.getBundleName())));
                    CatchBlockCreator c = et.addCatch(Throwable.class);
                    //c.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), c.getCaughtException());
                }
            }

            registerResourceBundles.returnVoid();
            overallCatch.invokeStaticMethod(registerResourceBundles.getMethodDescriptor());
        }

        CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
        print.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), print.getCaughtException());

        beforeAn.loadClassFromTCCL("io.quarkus.runner.ApplicationImpl");
        beforeAn.returnValue(null);

        file.close();
    }

}
