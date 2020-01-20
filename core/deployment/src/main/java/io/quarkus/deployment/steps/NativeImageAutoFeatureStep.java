package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DeploymentClassLoaderBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.ResourceHelper;

public class NativeImageAutoFeatureStep {

    private static final String GRAAL_AUTOFEATURE = "io/quarkus/runner/AutoFeature";
    private static final MethodDescriptor IMAGE_SINGLETONS_LOOKUP = ofMethod(ImageSingletons.class, "lookup", Object.class,
            Class.class);
    private static final MethodDescriptor INITIALIZE_AT_RUN_TIME = ofMethod(RuntimeClassInitialization.class,
            "initializeAtRunTime", void.class, Class[].class);
    private static final MethodDescriptor RERUN_INITIALIZATION = ofMethod(
            "org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport",
            "rerunInitialization", void.class, Class.class, String.class);
    static final String RUNTIME_REFLECTION = RuntimeReflection.class.getName();
    static final String BEFORE_ANALYSIS_ACCESS = Feature.BeforeAnalysisAccess.class.getName();
    static final String DYNAMIC_PROXY_REGISTRY = "com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry";
    static final String LOCALIZATION_FEATURE = "com.oracle.svm.core.jdk.LocalizationFeature";

    @BuildStep
    List<NativeImageResourceBuildItem> registerPackageResources(
            List<NativeImageResourceDirectoryBuildItem> nativeImageResourceDirectories,
            DeploymentClassLoaderBuildItem classLoader)
            throws IOException, URISyntaxException {
        List<NativeImageResourceBuildItem> resources = new ArrayList<>();

        for (NativeImageResourceDirectoryBuildItem nativeImageResourceDirectory : nativeImageResourceDirectories) {
            String path = classLoader.getClassLoader().getResource(nativeImageResourceDirectory.getPath()).getPath();
            File resourceFile = Paths.get(new URL(path.substring(0, path.indexOf("!"))).toURI()).toFile();
            try (JarFile jarFile = new JarFile(resourceFile)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String resourceName = entry.getName();
                    if (!entry.isDirectory() && resourceName.startsWith(nativeImageResourceDirectory.getPath())
                            && !resourceName.endsWith(".class")) {
                        resources.add(new NativeImageResourceBuildItem(resourceName));
                    }
                }
            }
        }

        return resources;
    }

    @BuildStep
    void generateFeature(BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItems,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClassBuildItems,
            List<NativeImageProxyDefinitionBuildItem> proxies,
            List<NativeImageResourceBuildItem> resources,
            List<NativeImageResourceBundleBuildItem> resourceBundles,
            List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<ServiceProviderBuildItem> serviceProviderBuildItems,
            List<UnsafeAccessedFieldBuildItem> unsafeAccessedFields) {
        ClassCreator file = new ClassCreator(new ClassOutput() {
            @Override
            public void write(String s, byte[] bytes) {
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
            overallCatch.invokeStaticMethod(INITIALIZE_AT_RUN_TIME, classes);
        }

        // hack in reinitialization of process info classes
        if (!runtimeReinitializedClassBuildItems.isEmpty()) {
            ResultHandle thisClass = overallCatch.loadClass(GRAAL_AUTOFEATURE);
            ResultHandle cl = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class),
                    thisClass);
            ResultHandle initSingleton = overallCatch.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                    overallCatch.loadClass("org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport"));
            ResultHandle quarkus = overallCatch.load("Quarkus");
            for (RuntimeReinitializedClassBuildItem runtimeReinitializedClass : runtimeReinitializedClassBuildItems) {
                TryBlock tc = overallCatch.tryBlock();
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(runtimeReinitializedClass.getClassName()), tc.load(false), cl);
                tc.invokeInterfaceMethod(RERUN_INITIALIZATION, initSingleton, clazz, quarkus);

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

        for (NativeImageResourceBuildItem i : resources) {
            for (String j : i.getResources()) {
                overallCatch.invokeStaticMethod(ofMethod(ResourceHelper.class, "registerResources", void.class, String.class),
                        overallCatch.load(j));
            }
        }

        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            overallCatch.invokeStaticMethod(ofMethod(ResourceHelper.class, "registerResources", void.class, String.class),
                    overallCatch.load(i.serviceDescriptorFile()));
        }

        if (!resourceBundles.isEmpty()) {
            ResultHandle locClass = overallCatch.loadClass(LOCALIZATION_FEATURE);

            ResultHandle params = overallCatch.marshalAsArray(Class.class, overallCatch.loadClass(String.class));
            ResultHandle registerMethod = overallCatch.invokeVirtualMethod(
                    ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), locClass,
                    overallCatch.load("addBundleToCache"), params);
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
        for (ReflectiveClassBuildItem i : reflectiveClassBuildItems) {
            addReflectiveClass(reflectiveClasses, i.isConstructors(), i.isMethods(), i.isFields(), i.areFinalFieldsWritable(),
                    i.isWeak(),
                    i.getClassNames().toArray(new String[0]));
        }
        for (ReflectiveFieldBuildItem i : reflectiveFields) {
            addReflectiveField(reflectiveClasses, i);
        }
        for (ReflectiveMethodBuildItem i : reflectiveMethods) {
            addReflectiveMethod(reflectiveClasses, i);
        }

        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            addReflectiveClass(reflectiveClasses, true, false, false, false, false,
                    i.providers().toArray(new String[] {}));
        }

        for (Map.Entry<String, ReflectionInfo> entry : reflectiveClasses.entrySet()) {

            MethodCreator mv = file.getMethodCreator("registerClass" + count++, "V");
            mv.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
            overallCatch.invokeStaticMethod(mv.getMethodDescriptor());

            TryBlock tc = mv.tryBlock();

            ResultHandle clazz = tc.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class),
                    tc.load(entry.getKey()));
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
            }

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
                tc.invokeStaticMethod(
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
            CatchBlockCreator cc = tc.addCatch(Throwable.class);
            //cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            mv.returnValue(null);
        }
        CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
        print.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), print.getCaughtException());

        beforeAn.loadClass("io.quarkus.runner.ApplicationImpl");
        beforeAn.returnValue(null);

        file.close();
    }

    public void addReflectiveMethod(Map<String, ReflectionInfo> reflectiveClasses, ReflectiveMethodBuildItem methodInfo) {
        String cl = methodInfo.getDeclaringClass();
        ReflectionInfo existing = reflectiveClasses.get(cl);
        if (existing == null) {
            reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false, false, false));
        }
        if (methodInfo.getName().equals("<init>")) {
            existing.ctorSet.add(methodInfo);
        } else {
            existing.methodSet.add(methodInfo);
        }
    }

    public void addReflectiveClass(Map<String, ReflectionInfo> reflectiveClasses, boolean constructors, boolean method,
            boolean fields, boolean finalFieldsWritable, boolean weak,
            String... className) {
        for (String cl : className) {
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, new ReflectionInfo(constructors, method, fields, finalFieldsWritable, weak));
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
            }
        }
    }

    public void addReflectiveField(Map<String, ReflectionInfo> reflectiveClasses, ReflectiveFieldBuildItem fieldInfo) {
        String cl = fieldInfo.getDeclaringClass();
        ReflectionInfo existing = reflectiveClasses.get(cl);
        if (existing == null) {
            reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false, false, false));
        }
        existing.fieldSet.add(fieldInfo.getName());
    }

    static final class ReflectionInfo {
        boolean constructors;
        boolean methods;
        boolean fields;
        boolean finalFieldsWritable;
        boolean weak;
        Set<String> fieldSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> methodSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> ctorSet = new HashSet<>();

        private ReflectionInfo(boolean constructors, boolean methods, boolean fields, boolean finalFieldsWritable,
                boolean weak) {
            this.methods = methods;
            this.fields = fields;
            this.constructors = constructors;
            this.finalFieldsWritable = finalFieldsWritable;
            this.weak = weak;
        }
    }
}
