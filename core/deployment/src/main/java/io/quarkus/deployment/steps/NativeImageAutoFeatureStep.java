package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.io.ObjectStreamClass;
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
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ForceNonWeakReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
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
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
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

    private static final Logger log = Logger.getLogger(NativeImageAutoFeatureStep.class);

    private static final String GRAAL_AUTOFEATURE = "io/quarkus/runner/AutoFeature";
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
    private static final MethodDescriptor RESOURCES_REGISTRY_ADD_RESOURCES = ofMethod(
            "com.oracle.svm.core.configure.ResourcesRegistry",
            "addResources", void.class, String.class);
    private static final MethodDescriptor RESOURCES_REGISTRY_IGNORE_RESOURCES = ofMethod(
            "com.oracle.svm.core.configure.ResourcesRegistry",
            "ignoreResources", void.class, String.class);
    static final String RUNTIME_REFLECTION = RuntimeReflection.class.getName();
    static final String JNI_RUNTIME_ACCESS = "com.oracle.svm.core.jni.JNIRuntimeAccess";
    static final String BEFORE_ANALYSIS_ACCESS = Feature.BeforeAnalysisAccess.class.getName();
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
            List<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses) {
        ClassCreator file = new ClassCreator(new ClassOutput() {
            @Override
            public void write(String s, byte[] bytes) {
                log.info("create GeneratedNativeImageClassBuildItem name=" + s);
                nativeImageClass.produce(new GeneratedNativeImageClassBuildItem(s, bytes));
            }
        }, GRAAL_AUTOFEATURE, null,
                Object.class.getName(), Feature.class.getName());
        file.addAnnotation("com.oracle.svm.core.annotate.AutomaticFeature");

        //MethodCreator afterReg = file.getMethodCreator("afterRegistration", void.class, "org.graalvm.nativeimage.Feature$AfterRegistrationAccess");
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
                overallCatch.loadClass("org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport"));
        overallCatch.invokeInterfaceMethod(BUILD_TIME_INITIALIZATION,
                imageSingleton,
                overallCatch.load(""), // empty string means everything
                overallCatch.load("Quarkus build time init default"));

        if (!runtimeInitializedClassBuildItems.isEmpty()) {
            ResultHandle thisClass = overallCatch.loadClass(GRAAL_AUTOFEATURE);
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
            ResultHandle thisClass = overallCatch.loadClass(GRAAL_AUTOFEATURE);
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
            ResultHandle proxySupportClass = overallCatch.loadClass(DYNAMIC_PROXY_REGISTRY);
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
                    overallCatch.loadClass("com.oracle.svm.core.configure.ResourcesRegistry"));
            TryBlock tc = overallCatch.tryBlock();
            for (NativeImageResourcePatternsBuildItem resourcePatternsItem : resourcePatterns) {
                for (String pattern : resourcePatternsItem.getExcludePatterns()) {
                    tc.invokeInterfaceMethod(RESOURCES_REGISTRY_IGNORE_RESOURCES, resourcesRegistrySingleton,
                            overallCatch.load(pattern));
                }
                for (String pattern : resourcePatternsItem.getIncludePatterns()) {
                    tc.invokeInterfaceMethod(
                            RESOURCES_REGISTRY_ADD_RESOURCES,
                            resourcesRegistrySingleton,
                            tc.load(pattern));
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
            ResultHandle legacyLocClass = locTryBlock.loadClass(LEGACY_LOCALIZATION_FEATURE);
            locTryBlock.assign(locClass, legacyLocClass);

            ResultHandle legacyParams = locTryBlock.marshalAsArray(Class.class, locTryBlock.loadClass(String.class));
            ResultHandle legacyRegisterMethod = locTryBlock.invokeVirtualMethod(
                    ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), legacyLocClass,
                    locTryBlock.load("addBundleToCache"), legacyParams);
            locTryBlock.assign(registerMethod, legacyRegisterMethod);

            CatchBlockCreator locCatchBlock = locTryBlock.addCatch(NoClassDefFoundError.class);
            ResultHandle newLocClass = locCatchBlock.loadClass(LOCALIZATION_FEATURE);
            locCatchBlock.assign(locClass, newLocClass);

            ResultHandle newParams = locCatchBlock.marshalAsArray(Class.class, locCatchBlock.loadClass(String.class));
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
                    log.info("invokeStaticMethod register constructors for " + entry.getKey());
                    tc.invokeStaticMethod(
                            ofMethod(RUNTIME_REFLECTION, "register", void.class, Executable[].class),
                            constructors);
                } else if (!entry.getValue().ctorSet.isEmpty()) {
                    ResultHandle farray = tc.newArray(Constructor.class, tc.load(1));
                    for (ReflectiveMethodBuildItem ctor : entry.getValue().ctorSet) {
                        ResultHandle paramArray = tc.newArray(Class.class, tc.load(ctor.getParams().length));
                        for (int i = 0; i < ctor.getParams().length; ++i) {
                            String type = ctor.getParams()[i];
                            tc.writeArrayValue(paramArray, i, tc.loadClass(type));
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
                            tc.writeArrayValue(paramArray, i, tc.loadClass(type));
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
                    BranchResult graalVm21Test = tc.ifGreaterEqualZero(
                            tc.invokeVirtualMethod(VERSION_COMPARE_TO,
                                    tc.invokeStaticMethod(VERSION_CURRENT),
                                    tc.marshalAsArray(int.class, tc.load(21))));
                    graalVm21Test.trueBranch().invokeStaticMethod(
                            ofMethod(RUNTIME_REFLECTION, "register", void.class,
                                    boolean.class, boolean.class, Field[].class),
                            tc.load(entry.getValue().finalFieldsWritable), tc.load(entry.getValue().serialization), fields);
                    graalVm21Test.falseBranch().invokeStaticMethod(
                            ofMethod(RUNTIME_REFLECTION, "register", void.class,
                                    boolean.class, Field[].class),
                            tc.load(entry.getValue().finalFieldsWritable), fields);
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

                log.info("invokeStaticMethod " + registerSerializationMethod.getDeclaringClass() + ":"
                        + registerSerializationMethod.getName() + " on " + entry.getKey());
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

        beforeAn.loadClass("io.quarkus.runner.ApplicationImpl");
        beforeAn.returnValue(null);

        file.close();
    }

    private MethodDescriptor createRegisterSerializationForClassMethod(ClassCreator file) {
        //register serialization feature as requested
        MethodCreator requiredFeatures = file.getMethodCreator("getRequiredFeatures", "java.util.List");

        TryBlock requiredCatch = requiredFeatures.tryBlock();

        // It's too early in the GraalVM build to use Version.getCurrent()
        BranchResult featuresGraalVm20Test = requiredCatch.ifTrue(requiredCatch
                .invokeVirtualMethod(ofMethod(String.class, "startsWith", boolean.class, String.class),
                        requiredCatch.invokeStaticMethod(
                                ofMethod(System.class, "getProperty", String.class, String.class),
                                requiredCatch.load("org.graalvm.version")),
                        requiredCatch.load("20.")));

        BytecodeCreator featuresIfGraalVM21Plus = featuresGraalVm20Test.falseBranch();

        ResultHandle serializationFeatureClass = featuresIfGraalVM21Plus
                .loadClass("com.oracle.svm.reflect.serialize.hosted.SerializationFeature");
        featuresIfGraalVM21Plus.returnValue(featuresIfGraalVM21Plus.invokeStaticMethod(
                ofMethod("java.util.Collections", "singletonList", List.class, Object.class),
                serializationFeatureClass));

        BytecodeCreator featuresIfNotGraalVM21Plus = featuresGraalVm20Test.trueBranch();
        featuresIfNotGraalVM21Plus
                .returnValue(featuresIfNotGraalVM21Plus
                        .invokeStaticMethod(ofMethod("java.util.Collections", "emptyList", List.class)));

        // method to register class for registration
        MethodCreator addSerializationForClass = file.getMethodCreator("registerSerializationForClass", "V", Class.class);
        addSerializationForClass.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        ResultHandle clazz = addSerializationForClass.getMethodParam(0);

        BranchResult graalVm21Test = addSerializationForClass.ifGreaterEqualZero(
                addSerializationForClass.invokeVirtualMethod(VERSION_COMPARE_TO,
                        addSerializationForClass.invokeStaticMethod(VERSION_CURRENT),
                        addSerializationForClass.marshalAsArray(int.class, addSerializationForClass.load(21))));

        TryBlock tc = graalVm21Test.trueBranch().tryBlock();

        ResultHandle currentThread = tc
                .invokeStaticMethod(ofMethod(Thread.class, "currentThread", Thread.class));
        ResultHandle tccl = tc.invokeVirtualMethod(
                ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                currentThread);

        MethodDescriptor forNameMethodDescriptor = ofMethod(Class.class, "forName", Class.class, String.class, boolean.class,
                ClassLoader.class);

        MethodDescriptor lookupMethod = ofMethod("com.oracle.svm.util.ReflectionUtil", "lookupMethod", Method.class,
                Class.class, String.class,
                Class[].class);
        MethodDescriptor invokeMethodDescriptor = ofMethod(Method.class, "invoke", Object.class, Object.class,
                Object[].class);

        BranchResult graalVm21_3Test = tc.ifGreaterEqualZero(
                tc.invokeVirtualMethod(VERSION_COMPARE_TO, tc.invokeStaticMethod(VERSION_CURRENT),
                        tc.marshalAsArray(int.class, tc.load(21), tc.load(3))));

        BytecodeCreator greaterThan21_3 = graalVm21_3Test.trueBranch();
        ResultHandle runtimeSerializationClass = greaterThan21_3.invokeStaticMethod(forNameMethodDescriptor,
                greaterThan21_3.load("org.graalvm.nativeimage.hosted.RuntimeSerialization"),
                greaterThan21_3.load(false), tccl);
        ResultHandle registerArgTypes = greaterThan21_3.newArray(Class.class, greaterThan21_3.load(1));
        greaterThan21_3.writeArrayValue(registerArgTypes, 0, greaterThan21_3.loadClass(Class[].class));
        ResultHandle registerLookupMethod = greaterThan21_3.invokeStaticMethod(lookupMethod, runtimeSerializationClass,
                greaterThan21_3.load("register"), registerArgTypes);
        ResultHandle registerArgs = greaterThan21_3.newArray(Object.class, greaterThan21_3.load(1));
        ResultHandle classesToRegister = greaterThan21_3.newArray(Class.class, greaterThan21_3.load(1));
        greaterThan21_3.writeArrayValue(classesToRegister, 0, clazz);
        greaterThan21_3.writeArrayValue(registerArgs, 0, classesToRegister);
        greaterThan21_3.invokeVirtualMethod(invokeMethodDescriptor, registerLookupMethod,
                greaterThan21_3.loadNull(), registerArgs);
        greaterThan21_3.returnValue(null);

        ResultHandle objectClass = tc.invokeStaticMethod(forNameMethodDescriptor, tc.load("java.lang.Object"),
                tc.load(false), tccl);
        ResultHandle serializationRegistryClass = tc.invokeStaticMethod(forNameMethodDescriptor,
                tc.load("com.oracle.svm.core.jdk.serialize.SerializationRegistry"),
                tc.load(false), tccl);
        ResultHandle addReflectionsClass = tc.invokeStaticMethod(forNameMethodDescriptor,
                tc.load("com.oracle.svm.reflect.serialize.hosted.SerializationFeature"),
                tc.load(false), tccl);

        ResultHandle serializationSupport = tc.invokeStaticMethod(
                IMAGE_SINGLETONS_LOOKUP,
                serializationRegistryClass);

        ResultHandle addReflectionsLookupArgs = tc.newArray(Class.class, tc.load(2));
        tc.writeArrayValue(addReflectionsLookupArgs, 0, tc.loadClass(Class.class));
        tc.writeArrayValue(addReflectionsLookupArgs, 1, tc.loadClass(Class.class));
        ResultHandle addReflectionsLookupMethod = tc.invokeStaticMethod(lookupMethod, addReflectionsClass,
                tc.load("addReflections"), addReflectionsLookupArgs);

        ResultHandle reflectionFactory = tc.invokeStaticMethod(
                ofMethod("sun.reflect.ReflectionFactory", "getReflectionFactory", "sun.reflect.ReflectionFactory"));

        AssignableResultHandle newSerializationConstructor = tc.createVariable(Constructor.class);

        ResultHandle externalizableClass = tc.invokeStaticMethod(
                forNameMethodDescriptor,
                tc.load("java.io.Externalizable"), tc.load(false), tccl);

        BranchResult isExternalizable = tc
                .ifTrue(tc.invokeVirtualMethod(ofMethod(Class.class, "isAssignableFrom", boolean.class, Class.class),
                        externalizableClass, clazz));
        BytecodeCreator ifIsExternalizable = isExternalizable.trueBranch();

        ResultHandle array1 = ifIsExternalizable.newArray(Class.class, tc.load(1));
        ResultHandle classClass = ifIsExternalizable.invokeStaticMethod(
                forNameMethodDescriptor,
                ifIsExternalizable.load("java.lang.Class"), ifIsExternalizable.load(false), tccl);
        ifIsExternalizable.writeArrayValue(array1, 0, classClass);

        ResultHandle externalizableLookupMethod = ifIsExternalizable.invokeStaticMethod(
                lookupMethod,
                ifIsExternalizable.loadClass(ObjectStreamClass.class), ifIsExternalizable.load("getExternalizableConstructor"),
                array1);

        ResultHandle array2 = ifIsExternalizable.newArray(Object.class, tc.load(1));
        ifIsExternalizable.writeArrayValue(array2, 0, clazz);

        ResultHandle externalizableConstructor = ifIsExternalizable.invokeVirtualMethod(
                invokeMethodDescriptor, externalizableLookupMethod, ifIsExternalizable.loadNull(), array2);

        ResultHandle externalizableConstructorClass = ifIsExternalizable.invokeVirtualMethod(
                ofMethod(Constructor.class, "getDeclaringClass", Class.class),
                externalizableConstructor);

        ResultHandle addReflectionsArgs1 = ifIsExternalizable.newArray(Class.class, tc.load(2));
        ifIsExternalizable.writeArrayValue(addReflectionsArgs1, 0, clazz);
        ifIsExternalizable.writeArrayValue(addReflectionsArgs1, 1, externalizableConstructorClass);
        ifIsExternalizable.invokeVirtualMethod(invokeMethodDescriptor, addReflectionsLookupMethod,
                ifIsExternalizable.loadNull(), addReflectionsArgs1);

        ifIsExternalizable.returnValue(null);

        ResultHandle clazzModifiers = tc
                .invokeVirtualMethod(ofMethod(Class.class, "getModifiers", int.class), clazz);
        BranchResult isAbstract = tc.ifTrue(tc
                .invokeStaticMethod(ofMethod(Modifier.class, "isAbstract", boolean.class, int.class), clazzModifiers));

        BytecodeCreator ifIsAbstract = isAbstract.trueBranch();
        BytecodeCreator ifNotAbstract = isAbstract.falseBranch();

        //abstract classes uses SerializationSupport$StubForAbstractClass for constructor
        ResultHandle stubConstructor = ifIsAbstract.invokeVirtualMethod(
                ofMethod("sun.reflect.ReflectionFactory", "newConstructorForSerialization", Constructor.class,
                        Class.class),
                reflectionFactory,
                tc
                        .loadClass("com.oracle.svm.reflect.serialize.SerializationSupport$StubForAbstractClass"));
        ifIsAbstract.assign(newSerializationConstructor, stubConstructor);

        ResultHandle classConstructor = ifNotAbstract.invokeVirtualMethod(
                ofMethod("sun.reflect.ReflectionFactory", "newConstructorForSerialization", Constructor.class,
                        Class.class),
                reflectionFactory, clazz);
        ifNotAbstract.assign(newSerializationConstructor, classConstructor);

        ResultHandle newSerializationConstructorClass = tc.invokeVirtualMethod(
                ofMethod(Constructor.class, "getDeclaringClass", Class.class),
                newSerializationConstructor);

        ResultHandle getConstructorAccessor = tc.invokeStaticMethod(
                lookupMethod, tc.loadClass(Constructor.class), tc.load("getConstructorAccessor"),
                tc.newArray(Class.class, tc.load(0)));

        ResultHandle accessor = tc.invokeVirtualMethod(
                invokeMethodDescriptor, getConstructorAccessor, newSerializationConstructor,
                tc.newArray(Object.class, tc.load(0)));

        tc.invokeVirtualMethod(
                ofMethod("com.oracle.svm.reflect.serialize.SerializationSupport", "addConstructorAccessor",
                        Object.class, Class.class, Class.class, Object.class),
                serializationSupport, clazz, newSerializationConstructorClass, accessor);
        ResultHandle addReflectionsArgs2 = tc.newArray(Class.class, tc.load(2));
        tc.writeArrayValue(addReflectionsArgs2, 0, clazz);
        tc.writeArrayValue(addReflectionsArgs2, 1, objectClass);
        tc.invokeVirtualMethod(invokeMethodDescriptor, addReflectionsLookupMethod, tc.loadNull(), addReflectionsArgs2);

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
