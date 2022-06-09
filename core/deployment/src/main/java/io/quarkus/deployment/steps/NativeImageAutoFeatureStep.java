package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.home.Version;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ForceNonWeakReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.ResourceHelper;
import io.quarkus.runtime.graal.ResourcesFeature;
import io.quarkus.runtime.graal.WeakReflection;

public class NativeImageAutoFeatureStep {

    private static final String GRAAL_AUTOFEATURE = "io.quarkus.runner.AutoFeature";
    private static final MethodDescriptor VERSION_CURRENT = ofMethod(Version.class, "getCurrent", Version.class);
    private static final MethodDescriptor VERSION_COMPARE_TO = ofMethod(Version.class, "compareTo", int.class, int[].class);

    private static final MethodDescriptor IMAGE_SINGLETONS_LOOKUP = ofMethod(ImageSingletons.class, "lookup", Object.class,
            Class.class);
    private static final MethodDescriptor BUILD_TIME_INITIALIZATION = ofMethod(
            "org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport",
            "initializeAtBuildTime", void.class, String.class, String.class);
    private static final MethodDescriptor INITIALIZE_CLASSES_AT_RUN_TIME = ofMethod(RuntimeClassInitialization.class,
            "initializeAtRunTime", void.class, Class[].class);
    private static final MethodDescriptor INITIALIZE_PACKAGES_AT_RUN_TIME = ofMethod(RuntimeClassInitialization.class,
            "initializeAtRunTime", void.class, String[].class);
    private static final MethodDescriptor RERUN_INITIALIZATION = ofMethod(
            "org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport",
            "rerunInitialization", void.class, Class.class, String.class);

    private static final MethodDescriptor CONFIGURATION_ALWAYS_TRUE = ofMethod(
            "org.graalvm.nativeimage.impl.ConfigurationCondition",
            "alwaysTrue", "org.graalvm.nativeimage.impl.ConfigurationCondition");

    private static final MethodDescriptor REGISTER_LAMBDA_CAPTURING_CLASS = ofMethod(
            "org.graalvm.nativeimage.impl.RuntimeSerializationSupport",
            "registerLambdaCapturingClass", void.class,
            "org.graalvm.nativeimage.impl.ConfigurationCondition",
            String.class);

    private static final MethodDescriptor LOOKUP_METHOD = ofMethod(
            "com.oracle.svm.util.ReflectionUtil",
            "lookupMethod", Method.class, Class.class, String.class, Class[].class);
    private static final MethodDescriptor FOR_NAME = ofMethod(
            Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class);
    private static final MethodDescriptor INVOKE = ofMethod(
            Method.class, "invoke", Object.class, Object.class, Object[].class);
    static final String RUNTIME_REFLECTION = RuntimeReflection.class.getName();
    static final String JNI_RUNTIME_ACCESS = "com.oracle.svm.core.jni.JNIRuntimeAccess";
    static final String BEFORE_ANALYSIS_ACCESS = Feature.BeforeAnalysisAccess.class.getName();
    static final String DURING_SETUP_ACCESS = Feature.DuringSetupAccess.class.getName();
    static final String DYNAMIC_PROXY_REGISTRY = "com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry";
    static final String LEGACY_LOCALIZATION_FEATURE = "com.oracle.svm.core.jdk.LocalizationFeature";
    static final String LOCALIZATION_FEATURE = "com.oracle.svm.core.jdk.localization.LocalizationFeature";
    public static final MethodDescriptor WEAK_REFLECTION_REGISTRATION = MethodDescriptor.ofMethod(WeakReflection.class,
            "register", void.class, Feature.BeforeAnalysisAccess.class, Class.class, boolean.class, boolean.class,
            boolean.class);

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
    void generateFeature(BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItems,
            List<RuntimeInitializedPackageBuildItem> runtimeInitializedPackageBuildItems,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClassBuildItems,
            List<NativeImageProxyDefinitionBuildItem> proxies,
            List<NativeImageResourcePatternsBuildItem> resourcePatterns,
            List<NativeImageResourceBundleBuildItem> resourceBundles,
            List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<ForceNonWeakReflectiveClassBuildItem> nonWeakReflectiveClassBuildItems,
            List<ServiceProviderBuildItem> serviceProviderBuildItems,
            List<UnsafeAccessedFieldBuildItem> unsafeAccessedFields,
            List<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses,
            List<LambdaCapturingTypeBuildItem> lambdaCapturingTypeBuildItems) {
        ClassCreator file = new ClassCreator(new ClassOutput() {
            @Override
            public void write(String s, byte[] bytes) {
                nativeImageClass.produce(new GeneratedNativeImageClassBuildItem(s, bytes));
            }
        }, GRAAL_AUTOFEATURE, null,
                Object.class.getName(), Feature.class.getName());
        file.addAnnotation("com.oracle.svm.core.annotate.AutomaticFeature");

        MethodCreator duringSetup = file.getMethodCreator("duringSetup", "V", DURING_SETUP_ACCESS);
        // Register Lambda Capturing Types
        if (!lambdaCapturingTypeBuildItems.isEmpty()) {
            ResultHandle runtimeSerializationSupportSingleton = duringSetup.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                    duringSetup.loadClassFromTCCL("org.graalvm.nativeimage.impl.RuntimeSerializationSupport"));
            ResultHandle configAlwaysTrue = duringSetup.invokeStaticMethod(CONFIGURATION_ALWAYS_TRUE);

            for (LambdaCapturingTypeBuildItem i : lambdaCapturingTypeBuildItems) {
                TryBlock tryBlock = duringSetup.tryBlock();

                tryBlock.invokeInterfaceMethod(REGISTER_LAMBDA_CAPTURING_CLASS, runtimeSerializationSupportSingleton,
                        configAlwaysTrue,
                        tryBlock.load(i.getClassName()));

                CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
                catchBlock.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class),
                        catchBlock.getCaughtException());
            }
        }
        duringSetup.returnValue(null);

        MethodCreator beforeAn = file.getMethodCreator("beforeAnalysis", "V", BEFORE_ANALYSIS_ACCESS);
        TryBlock overallCatch = beforeAn.tryBlock();
        //TODO: at some point we are going to need to break this up, as if it get too big it will hit the method size limit

        ResultHandle beforeAnalysisParam = beforeAn.getMethodParam(0);
        for (UnsafeAccessedFieldBuildItem unsafeAccessedField : unsafeAccessedFields) {
            TryBlock tc = overallCatch.tryBlock();
            ResultHandle declaringClassHandle = tc.invokeStaticMethod(
                    ofMethod(Class.class, "forName", Class.class, String.class),
                    tc.load(unsafeAccessedField.getDeclaringClass()));
            ResultHandle fieldHandle = tc.invokeVirtualMethod(
                    ofMethod(Class.class, "getDeclaredField", Field.class, String.class), declaringClassHandle,
                    tc.load(unsafeAccessedField.getFieldName()));
            tc.invokeInterfaceMethod(
                    ofMethod(Feature.BeforeAnalysisAccess.class, "registerAsUnsafeAccessed", void.class, Field.class),
                    beforeAnalysisParam, fieldHandle);
            CatchBlockCreator cc = tc.addCatch(Throwable.class);
            cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
        }

        ResultHandle imageSingleton = overallCatch.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                overallCatch.loadClassFromTCCL("org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport"));
        overallCatch.invokeInterfaceMethod(BUILD_TIME_INITIALIZATION,
                imageSingleton,
                overallCatch.load(""), // empty string means everything
                overallCatch.load("Quarkus build time init default"));

        if (!runtimeInitializedClassBuildItems.isEmpty()) {
            ResultHandle thisClass = overallCatch.loadClassFromTCCL(GRAAL_AUTOFEATURE);
            ResultHandle cl = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class),
                    thisClass);
            ResultHandle classes = overallCatch.newArray(Class.class,
                    overallCatch.load(runtimeInitializedClassBuildItems.size()));
            for (int i = 0; i < runtimeInitializedClassBuildItems.size(); i++) {
                TryBlock tc = overallCatch.tryBlock();
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(runtimeInitializedClassBuildItems.get(i).getClassName()), tc.load(false), cl);
                tc.writeArrayValue(classes, i, clazz);
                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }
            overallCatch.invokeStaticMethod(INITIALIZE_CLASSES_AT_RUN_TIME, classes);
        }

        if (!runtimeInitializedPackageBuildItems.isEmpty()) {
            ResultHandle packages = overallCatch.newArray(String.class,
                    overallCatch.load(runtimeInitializedPackageBuildItems.size()));
            for (int i = 0; i < runtimeInitializedPackageBuildItems.size(); i++) {
                TryBlock tc = overallCatch.tryBlock();
                ResultHandle pkg = tc.load(runtimeInitializedPackageBuildItems.get(i).getPackageName());
                tc.writeArrayValue(packages, i, pkg);
                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }
            overallCatch.invokeStaticMethod(INITIALIZE_PACKAGES_AT_RUN_TIME, packages);
        }

        // hack in reinitialization of process info classes
        if (!runtimeReinitializedClassBuildItems.isEmpty()) {
            ResultHandle thisClass = overallCatch.loadClassFromTCCL(GRAAL_AUTOFEATURE);
            ResultHandle cl = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class),
                    thisClass);
            ResultHandle quarkus = overallCatch.load("Quarkus");
            for (RuntimeReinitializedClassBuildItem runtimeReinitializedClass : runtimeReinitializedClassBuildItems) {
                TryBlock tc = overallCatch.tryBlock();
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(runtimeReinitializedClass.getClassName()), tc.load(false), cl);
                tc.invokeInterfaceMethod(RERUN_INITIALIZATION, imageSingleton, clazz, quarkus);

                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }
        }

        if (!proxies.isEmpty()) {
            ResultHandle proxySupportClass = overallCatch.loadClassFromTCCL(DYNAMIC_PROXY_REGISTRY);
            ResultHandle proxySupport = overallCatch.invokeStaticMethod(
                    IMAGE_SINGLETONS_LOOKUP,
                    proxySupportClass);
            for (NativeImageProxyDefinitionBuildItem proxy : proxies) {
                ResultHandle array = overallCatch.newArray(Class.class, overallCatch.load(proxy.getClasses().size()));
                int i = 0;
                for (String p : proxy.getClasses()) {
                    ResultHandle clazz = overallCatch.invokeStaticMethod(
                            ofMethod(Class.class, "forName", Class.class, String.class), overallCatch.load(p));
                    overallCatch.writeArrayValue(array, i++, clazz);

                }
                overallCatch.invokeInterfaceMethod(ofMethod(DYNAMIC_PROXY_REGISTRY,
                        "addProxyClass", void.class, Class[].class), proxySupport, array);
            }
        }

        /* Resource includes and excludes */
        if (!resourcePatterns.isEmpty()) {
            ResultHandle resourcesRegistrySingleton = overallCatch.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                    overallCatch.loadClassFromTCCL("com.oracle.svm.core.configure.ResourcesRegistry"));
            TryBlock tc = overallCatch.tryBlock();

            ResultHandle currentThread = tc.invokeStaticMethod(ofMethod(Thread.class, "currentThread", Thread.class));
            ResultHandle tccl = tc.invokeVirtualMethod(ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                    currentThread);
            AssignableResultHandle resourcesArgTypes = tc.createVariable(Class[].class);
            AssignableResultHandle resourcesArgs = tc.createVariable(Object[].class);
            AssignableResultHandle argsIndex = tc.createVariable(int.class);

            ResultHandle argTypes = tc.newArray(Class.class, tc.load(2));
            ResultHandle configurationConditionClass = tc.invokeStaticMethod(FOR_NAME,
                    tc.load("org.graalvm.nativeimage.impl.ConfigurationCondition"),
                    tc.load(false), tccl);
            tc.writeArrayValue(argTypes, 0, configurationConditionClass);
            tc.writeArrayValue(argTypes, 1, tc.loadClassFromTCCL(String.class));
            tc.assign(resourcesArgTypes, argTypes);
            ResultHandle args = tc.newArray(Object.class, tc.load(2));
            ResultHandle alwaysTrueMethod = tc.invokeStaticMethod(LOOKUP_METHOD,
                    configurationConditionClass,
                    tc.load("alwaysTrue"),
                    tc.newArray(Class.class, tc.load(0)));
            ResultHandle alwaysTrueResult = tc.invokeVirtualMethod(INVOKE,
                    alwaysTrueMethod, tc.loadNull(),
                    tc.newArray(Object.class, tc.load(0)));
            tc.writeArrayValue(args, 0, alwaysTrueResult);
            tc.assign(resourcesArgs, args);
            tc.assign(argsIndex, tc.load(1));

            ResultHandle ignoreResourcesMethod = tc.invokeStaticMethod(LOOKUP_METHOD,
                    tc.loadClassFromTCCL("com.oracle.svm.core.configure.ResourcesRegistry"),
                    tc.load("ignoreResources"), resourcesArgTypes);
            ResultHandle addResourcesMethod = tc.invokeStaticMethod(LOOKUP_METHOD,
                    tc.loadClassFromTCCL("com.oracle.svm.core.configure.ResourcesRegistry"),
                    tc.load("addResources"), resourcesArgTypes);

            for (NativeImageResourcePatternsBuildItem resourcePatternsItem : resourcePatterns) {
                for (String pattern : resourcePatternsItem.getExcludePatterns()) {
                    tc.writeArrayValue(resourcesArgs, argsIndex, tc.load(pattern));
                    tc.invokeVirtualMethod(INVOKE, ignoreResourcesMethod, resourcesRegistrySingleton, resourcesArgs);
                }
                for (String pattern : resourcePatternsItem.getIncludePatterns()) {
                    tc.writeArrayValue(resourcesArgs, argsIndex, tc.load(pattern));
                    tc.invokeVirtualMethod(INVOKE, addResourcesMethod, resourcesRegistrySingleton, resourcesArgs);
                }
            }
            CatchBlockCreator cc = tc.addCatch(Throwable.class);
            cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
        }

        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            overallCatch.invokeStaticMethod(ofMethod(ResourceHelper.class, "registerResources", void.class, String.class),
                    overallCatch.load(i.serviceDescriptorFile()));
        }

        if (!resourceBundles.isEmpty()) {
            AssignableResultHandle registerMethod = overallCatch.createVariable(Method.class);
            AssignableResultHandle locClass = overallCatch.createVariable(Class.class);
            TryBlock locTryBlock = overallCatch.tryBlock();
            ResultHandle legacyLocClass = locTryBlock.loadClassFromTCCL(LEGACY_LOCALIZATION_FEATURE);
            locTryBlock.assign(locClass, legacyLocClass);

            ResultHandle legacyParams = locTryBlock.marshalAsArray(Class.class, locTryBlock.loadClassFromTCCL(String.class));
            ResultHandle legacyRegisterMethod = locTryBlock.invokeVirtualMethod(
                    ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), legacyLocClass,
                    locTryBlock.load("addBundleToCache"), legacyParams);
            locTryBlock.assign(registerMethod, legacyRegisterMethod);

            CatchBlockCreator locCatchBlock = locTryBlock.addCatch(ClassNotFoundException.class);
            ResultHandle newLocClass = locCatchBlock.loadClassFromTCCL(LOCALIZATION_FEATURE);
            locCatchBlock.assign(locClass, newLocClass);

            ResultHandle newParams = locCatchBlock.marshalAsArray(Class.class, locCatchBlock.loadClassFromTCCL(String.class));
            ResultHandle newRegisterMethod = locCatchBlock.invokeVirtualMethod(
                    ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), newLocClass,
                    locCatchBlock.load("prepareBundle"), newParams);
            locCatchBlock.assign(registerMethod, newRegisterMethod);

            overallCatch.invokeVirtualMethod(ofMethod(AccessibleObject.class, "setAccessible", void.class, boolean.class),
                    registerMethod, overallCatch.load(true));

            ResultHandle locSupport = overallCatch.invokeStaticMethod(
                    IMAGE_SINGLETONS_LOOKUP,
                    locClass);
            for (NativeImageResourceBundleBuildItem i : resourceBundles) {
                TryBlock et = overallCatch.tryBlock();

                et.invokeVirtualMethod(ofMethod(Method.class, "invoke", Object.class, Object.class, Object[].class),
                        registerMethod, locSupport, et.marshalAsArray(Object.class, et.load(i.getBundleName())));
                CatchBlockCreator c = et.addCatch(Throwable.class);
                //c.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), c.getCaughtException());
            }
        }
        int count = 0;

        final Map<String, ReflectionInfo> reflectiveClasses = new LinkedHashMap<>();
        final Set<String> forcedNonWeakClasses = new HashSet<>();
        for (ForceNonWeakReflectiveClassBuildItem nonWeakReflectiveClassBuildItem : nonWeakReflectiveClassBuildItems) {
            forcedNonWeakClasses.add(nonWeakReflectiveClassBuildItem.getClassName());
        }
        for (ReflectiveClassBuildItem i : reflectiveClassBuildItems) {
            addReflectiveClass(reflectiveClasses, forcedNonWeakClasses, i.isConstructors(), i.isMethods(), i.isFields(),
                    i.areFinalFieldsWritable(),
                    i.isWeak(),
                    i.isSerialization(),
                    i.getClassNames().toArray(new String[0]));
        }
        for (ReflectiveFieldBuildItem i : reflectiveFields) {
            addReflectiveField(reflectiveClasses, i);
        }
        for (ReflectiveMethodBuildItem i : reflectiveMethods) {
            addReflectiveMethod(reflectiveClasses, i);
        }

        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            addReflectiveClass(reflectiveClasses, forcedNonWeakClasses, true, false, false, false, false, false,
                    i.providers().toArray(new String[] {}));
        }

        MethodDescriptor registerSerializationMethod = null;

        for (Map.Entry<String, ReflectionInfo> entry : reflectiveClasses.entrySet()) {
            MethodCreator mv = file.getMethodCreator("registerClass" + count++, void.class, Feature.BeforeAnalysisAccess.class);
            mv.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
            overallCatch.invokeStaticMethod(mv.getMethodDescriptor(), overallCatch.getMethodParam(0));

            TryBlock tc = mv.tryBlock();

            ResultHandle currentThread = tc
                    .invokeStaticMethod(ofMethod(Thread.class, "currentThread", Thread.class));
            ResultHandle tccl = tc.invokeVirtualMethod(
                    ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                    currentThread);
            ResultHandle clazz = tc.invokeStaticMethod(
                    ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                    tc.load(entry.getKey()), tc.load(false), tccl);
            //we call these methods first, so if they are going to throw an exception it happens before anything has been registered
            ResultHandle constructors = tc
                    .invokeVirtualMethod(ofMethod(Class.class, "getDeclaredConstructors", Constructor[].class), clazz);
            ResultHandle methods = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredMethods", Method[].class), clazz);
            ResultHandle fields = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredFields", Field[].class), clazz);

            if (!entry.getValue().weak) {
                ResultHandle carray = tc.newArray(Class.class, tc.load(1));
                tc.writeArrayValue(carray, 0, clazz);
                tc.invokeStaticMethod(ofMethod(RUNTIME_REFLECTION, "register", void.class, Class[].class),
                        carray);

                if (entry.getValue().constructors) {
                    tc.invokeStaticMethod(
                            ofMethod(RUNTIME_REFLECTION, "register", void.class, Executable[].class),
                            constructors);
                } else if (!entry.getValue().ctorSet.isEmpty()) {
                    ResultHandle farray = tc.newArray(Constructor.class, tc.load(1));
                    for (ReflectiveMethodBuildItem ctor : entry.getValue().ctorSet) {
                        ResultHandle paramArray = tc.newArray(Class.class, tc.load(ctor.getParams().length));
                        for (int i = 0; i < ctor.getParams().length; ++i) {
                            String type = ctor.getParams()[i];
                            tc.writeArrayValue(paramArray, i, tc.loadClassFromTCCL(type));
                        }
                        ResultHandle fhandle = tc.invokeVirtualMethod(
                                ofMethod(Class.class, "getDeclaredConstructor", Constructor.class, Class[].class), clazz,
                                paramArray);
                        tc.writeArrayValue(farray, 0, fhandle);
                        tc.invokeStaticMethod(
                                ofMethod(RUNTIME_REFLECTION, "register", void.class, Executable[].class),
                                farray);
                    }
                }
                if (entry.getValue().methods) {
                    tc.invokeStaticMethod(
                            ofMethod(RUNTIME_REFLECTION, "register", void.class, Executable[].class),
                            methods);
                } else if (!entry.getValue().methodSet.isEmpty()) {
                    ResultHandle farray = tc.newArray(Method.class, tc.load(1));
                    for (ReflectiveMethodBuildItem method : entry.getValue().methodSet) {
                        ResultHandle paramArray = tc.newArray(Class.class, tc.load(method.getParams().length));
                        for (int i = 0; i < method.getParams().length; ++i) {
                            String type = method.getParams()[i];
                            tc.writeArrayValue(paramArray, i, tc.loadClassFromTCCL(type));
                        }
                        ResultHandle fhandle = tc.invokeVirtualMethod(
                                ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), clazz,
                                tc.load(method.getName()), paramArray);
                        tc.writeArrayValue(farray, 0, fhandle);
                        tc.invokeStaticMethod(
                                ofMethod(RUNTIME_REFLECTION, "register", void.class, Executable[].class),
                                farray);
                    }
                }
                if (entry.getValue().fields) {
                    tc.invokeStaticMethod(
                            ofMethod(RUNTIME_REFLECTION, "register", void.class,
                                    boolean.class, boolean.class, Field[].class),
                            tc.load(entry.getValue().finalFieldsWritable), tc.load(entry.getValue().serialization), fields);
                } else if (!entry.getValue().fieldSet.isEmpty()) {
                    ResultHandle farray = tc.newArray(Field.class, tc.load(1));
                    for (String field : entry.getValue().fieldSet) {
                        ResultHandle fhandle = tc.invokeVirtualMethod(
                                ofMethod(Class.class, "getDeclaredField", Field.class, String.class), clazz, tc.load(field));
                        tc.writeArrayValue(farray, 0, fhandle);
                        tc.invokeStaticMethod(
                                ofMethod(RUNTIME_REFLECTION, "register", void.class, Field[].class),
                                farray);
                    }
                }
            } else {
                tc.invokeStaticMethod(WEAK_REFLECTION_REGISTRATION, tc.getMethodParam(0), clazz,
                        tc.load(entry.getValue().constructors), tc.load(entry.getValue().methods),
                        tc.load(entry.getValue().fields));
            }

            if (entry.getValue().serialization) {
                if (registerSerializationMethod == null) {
                    registerSerializationMethod = createRegisterSerializationForClassMethod(file);
                }

                tc.invokeStaticMethod(registerSerializationMethod, clazz);
            }

            CatchBlockCreator cc = tc.addCatch(Throwable.class);
            //cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            mv.returnValue(null);
        }

        count = 0;

        for (JniRuntimeAccessBuildItem jniAccessible : jniRuntimeAccessibleClasses) {
            for (String className : jniAccessible.getClassNames()) {
                MethodCreator mv = file.getMethodCreator("registerJniAccessibleClass" + count++, "V");
                mv.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
                overallCatch.invokeStaticMethod(mv.getMethodDescriptor());

                TryBlock tc = mv.tryBlock();

                ResultHandle currentThread = tc
                        .invokeStaticMethod(ofMethod(Thread.class, "currentThread", Thread.class));
                ResultHandle tccl = tc.invokeVirtualMethod(
                        ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                        currentThread);
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(className), tc.load(false), tccl);
                //we call these methods first, so if they are going to throw an exception it happens before anything has been registered
                ResultHandle constructors = tc
                        .invokeVirtualMethod(ofMethod(Class.class, "getDeclaredConstructors", Constructor[].class), clazz);
                ResultHandle methods = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredMethods", Method[].class),
                        clazz);
                ResultHandle fields = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredFields", Field[].class), clazz);

                ResultHandle carray = tc.newArray(Class.class, tc.load(1));
                tc.writeArrayValue(carray, 0, clazz);
                tc.invokeStaticMethod(ofMethod(JNI_RUNTIME_ACCESS, "register", void.class, Class[].class),
                        carray);

                if (jniAccessible.isConstructors()) {
                    tc.invokeStaticMethod(
                            ofMethod(JNI_RUNTIME_ACCESS, "register", void.class, Executable[].class),
                            constructors);
                }

                if (jniAccessible.isMethods()) {
                    tc.invokeStaticMethod(
                            ofMethod(JNI_RUNTIME_ACCESS, "register", void.class, Executable[].class),
                            methods);
                }

                if (jniAccessible.isFields()) {
                    tc.invokeStaticMethod(
                            ofMethod(JNI_RUNTIME_ACCESS, "register", void.class,
                                    boolean.class, Field[].class),
                            tc.load(jniAccessible.isFinalFieldsWriteable()), fields);
                }

                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                //cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
                mv.returnValue(null);
            }
        }

        CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
        print.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), print.getCaughtException());

        beforeAn.loadClassFromTCCL("io.quarkus.runner.ApplicationImpl");
        beforeAn.returnValue(null);

        file.close();
    }

    private MethodDescriptor createRegisterSerializationForClassMethod(ClassCreator file) {
        //register serialization feature as requested
        MethodCreator requiredFeatures = file.getMethodCreator("getRequiredFeatures", "java.util.List");

        TryBlock requiredCatch = requiredFeatures.tryBlock();

        ResultHandle serializationFeatureClass = requiredCatch
                .loadClassFromTCCL("com.oracle.svm.reflect.serialize.hosted.SerializationFeature");
        ResultHandle requiredFeaturesList = requiredCatch.invokeStaticMethod(
                ofMethod("java.util.Collections", "singletonList", List.class, Object.class),
                serializationFeatureClass);

        requiredCatch.returnValue(requiredFeaturesList);

        // method to register class for registration
        MethodCreator addSerializationForClass = file.getMethodCreator("registerSerializationForClass", "V", Class.class);
        addSerializationForClass.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        ResultHandle clazz = addSerializationForClass.getMethodParam(0);

        TryBlock tc = addSerializationForClass.tryBlock();

        ResultHandle currentThread = tc
                .invokeStaticMethod(ofMethod(Thread.class, "currentThread", Thread.class));
        ResultHandle tccl = tc.invokeVirtualMethod(
                ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                currentThread);

        ResultHandle runtimeSerializationClass = tc.invokeStaticMethod(FOR_NAME,
                tc.load("org.graalvm.nativeimage.hosted.RuntimeSerialization"),
                tc.load(false), tccl);
        ResultHandle registerArgTypes = tc.newArray(Class.class, tc.load(1));
        tc.writeArrayValue(registerArgTypes, 0, tc.loadClassFromTCCL(Class[].class));
        ResultHandle registerLookupMethod = tc.invokeStaticMethod(LOOKUP_METHOD, runtimeSerializationClass,
                tc.load("register"), registerArgTypes);
        ResultHandle registerArgs = tc.newArray(Object.class, tc.load(1));
        ResultHandle classesToRegister = tc.newArray(Class.class, tc.load(1));
        tc.writeArrayValue(classesToRegister, 0, clazz);
        tc.writeArrayValue(registerArgs, 0, classesToRegister);
        tc.invokeVirtualMethod(INVOKE, registerLookupMethod,
                tc.loadNull(), registerArgs);
        tc.returnValue(null);

        addSerializationForClass.returnValue(null);

        return addSerializationForClass.getMethodDescriptor();
    }

    public void addReflectiveMethod(Map<String, ReflectionInfo> reflectiveClasses, ReflectiveMethodBuildItem methodInfo) {
        String cl = methodInfo.getDeclaringClass();
        ReflectionInfo existing = reflectiveClasses.get(cl);
        if (existing == null) {
            reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false, false, false, false));
        }
        if (methodInfo.getName().equals("<init>")) {
            existing.ctorSet.add(methodInfo);
        } else {
            existing.methodSet.add(methodInfo);
        }
    }

    public void addReflectiveClass(Map<String, ReflectionInfo> reflectiveClasses, Set<String> forcedNonWeakClasses,
            boolean constructors, boolean method,
            boolean fields, boolean finalFieldsWritable, boolean weak, boolean serialization,
            String... className) {
        for (String cl : className) {
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, new ReflectionInfo(constructors, method, fields, finalFieldsWritable,
                        !forcedNonWeakClasses.contains(cl) && weak, serialization));
            } else {
                if (constructors) {
                    existing.constructors = true;
                }
                if (method) {
                    existing.methods = true;
                }
                if (fields) {
                    existing.fields = true;
                }
                if (serialization) {
                    existing.serialization = true;
                }
            }
        }
    }

    public void addReflectiveField(Map<String, ReflectionInfo> reflectiveClasses, ReflectiveFieldBuildItem fieldInfo) {
        String cl = fieldInfo.getDeclaringClass();
        ReflectionInfo existing = reflectiveClasses.get(cl);
        if (existing == null) {
            reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false, false, false, false));
        }
        existing.fieldSet.add(fieldInfo.getName());
    }

    static final class ReflectionInfo {
        boolean constructors;
        boolean methods;
        boolean fields;
        boolean finalFieldsWritable;
        boolean weak;
        boolean serialization;
        Set<String> fieldSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> methodSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> ctorSet = new HashSet<>();

        private ReflectionInfo(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWritable,
                boolean weak, boolean serialization) {
            this.methods = methods;
            this.fields = fields;
            this.constructors = constructors;
            this.finalFieldsWritable = finalFieldsWritable;
            this.weak = weak;
            this.serialization = serialization;
        }
    }

}
