package io.quarkus.deployment.steps;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ClassOutputBuildItem;
import io.quarkus.deployment.builditem.substrate.*;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.ResourceHelper;

public class SubstrateAutoFeatureStep {

    private static final String GRAAL_AUTOFEATURE = "io/quarkus/runner/AutoFeature";
    private static final MethodDescriptor IMAGE_SINGLETONS_LOOKUP = ofMethod(ImageSingletons.class, "lookup", Object.class,
            Class.class);
    private static final MethodDescriptor INITIALIZE_AT_RUN_TIME = ofMethod(RuntimeClassInitializationSupport.class,
            "initializeAtRunTime", void.class, Class.class, String.class);
    private static final MethodDescriptor RERUN_INITIALIZATION = ofMethod(RuntimeClassInitializationSupport.class,
            "rerunInitialization", void.class, Class.class, String.class);
    static final String RUNTIME_REFLECTION = RuntimeReflection.class.getName();
    static final String BEFORE_ANALYSIS_ACCESS = Feature.BeforeAnalysisAccess.class.getName();
    static final String DYNAMIC_PROXY_REGISTRY = "com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry";
    static final String LOCALIZATION_SUPPORT = "com.oracle.svm.core.jdk.LocalizationSupport";

    @BuildStep
    SubstrateOutputBuildItem generateFeature(ClassOutputBuildItem output,
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItems,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClassBuildItems,
            List<SubstrateProxyDefinitionBuildItem> proxies,
            List<SubstrateResourceBuildItem> resources,
            List<SubstrateResourceBundleBuildItem> resourceBundles,
            List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<ServiceProviderBuildItem> serviceProviderBuildItems) {
        ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(output.getClassOutput(), true), GRAAL_AUTOFEATURE, null,
                Object.class.getName(), Feature.class.getName());
        file.addAnnotation("com.oracle.svm.core.annotate.AutomaticFeature");

        //MethodCreator afterReg = file.getMethodCreator("afterRegistration", void.class, "org.graalvm.nativeimage.Feature$AfterRegistrationAccess");
        MethodCreator beforeAn = file.getMethodCreator("beforeAnalysis", "V", BEFORE_ANALYSIS_ACCESS);
        TryBlock overallCatch = beforeAn.tryBlock();
        //TODO: at some point we are going to need to break this up, as if it get too big it will hit the method size limit

        ResultHandle initSingleton = overallCatch.invokeStaticMethod(IMAGE_SINGLETONS_LOOKUP,
                overallCatch.loadClass(RuntimeClassInitializationSupport.class));
        ResultHandle quarkus = overallCatch.load("Quarkus");

        if (!runtimeInitializedClassBuildItems.isEmpty()) {
            ResultHandle thisClass = overallCatch.loadClass(GRAAL_AUTOFEATURE);
            ResultHandle cl = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class),
                    thisClass);
            for (String i : runtimeInitializedClassBuildItems.stream().map(RuntimeInitializedClassBuildItem::getClassName)
                    .collect(Collectors.toList())) {
                TryBlock tc = overallCatch.tryBlock();
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(i), tc.load(false), cl);
                tc.invokeInterfaceMethod(INITIALIZE_AT_RUN_TIME, initSingleton, clazz, quarkus);

                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
            }

        }

        // hack in reinitialization of process info classes
        {
            ResultHandle thisClass = overallCatch.loadClass(GRAAL_AUTOFEATURE);
            ResultHandle cl = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class),
                    thisClass);
            for (String i : runtimeReinitializedClassBuildItems.stream().map(RuntimeReinitializedClassBuildItem::getClassName)
                    .collect(Collectors.toList())) {
                TryBlock tc = overallCatch.tryBlock();
                ResultHandle clazz = tc.invokeStaticMethod(
                        ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                        tc.load(i), tc.load(false), cl);
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
            for (SubstrateProxyDefinitionBuildItem proxy : proxies) {
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

        for (SubstrateResourceBuildItem i : resources) {
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
            ResultHandle locClass = overallCatch.loadClass(LOCALIZATION_SUPPORT);

            ResultHandle params = overallCatch.marshalAsArray(Class.class, overallCatch.loadClass(String.class));
            ResultHandle registerMethod = overallCatch.invokeVirtualMethod(
                    ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), locClass,
                    overallCatch.load("addBundleToCache"), params);
            overallCatch.invokeVirtualMethod(ofMethod(AccessibleObject.class, "setAccessible", void.class, boolean.class),
                    registerMethod, overallCatch.load(true));

            ResultHandle locSupport = overallCatch.invokeStaticMethod(
                    IMAGE_SINGLETONS_LOOKUP,
                    locClass);
            for (SubstrateResourceBundleBuildItem i : resourceBundles) {
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
        return new SubstrateOutputBuildItem();
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
