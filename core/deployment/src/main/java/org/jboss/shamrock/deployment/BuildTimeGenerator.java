package org.jboss.shamrock.deployment;

import static org.jboss.protean.gizmo.MethodDescriptor.ofConstructor;
import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.UnresolvedTypeVariable;
import org.jboss.jandex.VoidType;
import org.jboss.protean.gizmo.CatchBlockCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.protean.gizmo.TryBlock;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorderImpl;
import org.jboss.shamrock.deployment.index.ApplicationArchiveLoader;
import org.jboss.shamrock.runtime.ResourceHelper;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;
import org.jboss.shamrock.runtime.Timing;
import org.objectweb.asm.ClassVisitor;

/**
 * Class that does the build time processing
 */
public class BuildTimeGenerator {

    private static final Logger log = Logger.getLogger(BuildTimeGenerator.class.getName());

    private static final AtomicInteger COUNT = new AtomicInteger();
    public static final String MAIN_CLASS_INTERNAL = "org/jboss/shamrock/runner/GeneratedMain";
    public static final String MAIN_CLASS = MAIN_CLASS_INTERNAL.replace('/', '.');
    private static final String GRAAL_AUTOFEATURE = "org/jboss/shamrock/runner/AutoFeature";
    private static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";

    private final List<ResourceProcessor> processors;
    private final ClassOutput output;
    private final DeploymentProcessorInjection injection;
    private final ClassLoader classLoader;
    private final boolean useStaticInit;
    private final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> byteCodeTransformers = new HashMap<>();
    private final Set<String> applicationArchiveMarkers;
    private final ArchiveContextBuilder archiveContextBuilder;
    private final Set<String> capabilities;

    public BuildTimeGenerator(ClassOutput classOutput, ClassLoader cl, boolean useStaticInit, ArchiveContextBuilder contextBuilder) {
        this.useStaticInit = useStaticInit;
        Iterator<ShamrockSetup> loader = ServiceLoader.load(ShamrockSetup.class, cl).iterator();
        SetupContextImpl setupContext = new SetupContextImpl();
        while (loader.hasNext()) {
            final ShamrockSetup setup = loader.next();
            log.log(Level.FINE, "Loading Shamrock setup extension: " + setup.getClass());
            setup.setup(setupContext);
        }
        setupContext.resourceProcessors.sort(Comparator.comparingInt(ResourceProcessor::getPriority));
        this.processors = Collections.unmodifiableList(setupContext.resourceProcessors);
        this.output = classOutput;
        this.injection = new DeploymentProcessorInjection(setupContext.injectionProviders);
        this.classLoader = cl;
        this.applicationArchiveMarkers = new HashSet<>(setupContext.applicationArchiveMarkers);
        this.archiveContextBuilder = contextBuilder;
        this.capabilities = new HashSet<>(setupContext.capabilities);
    }

    public Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> getByteCodeTransformers() {
        return byteCodeTransformers;
    }

    public void run(Path root) throws IOException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        BuildConfig config = BuildConfig.readConfig(classLoader, root.toFile());
        try {
            Indexer indexer = new Indexer();
            Files.walkFileTree(root, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) {
                        try (InputStream stream = Files.newInputStream(file)) {
                            indexer.index(stream);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
            Index appIndex = indexer.complete();
            List<ApplicationArchive> applicationArchives = ApplicationArchiveLoader.scanForOtherIndexes(classLoader, config, applicationArchiveMarkers, root, archiveContextBuilder.getAdditionalApplicationArchives());

            ArchiveContextImpl context = new ArchiveContextImpl(new ApplicationArchiveImpl(appIndex, root, null), applicationArchives, config);

            ProcessorContextImpl processorContext = new ProcessorContextImpl(context);
            processorContext.addResource("META-INF/microprofile-config.properties");
            try {
                for (ResourceProcessor processor : processors) {
                    try {
                        injection.injectClass(processor);
                        processor.process(context, processorContext);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                processorContext.writeProperties(root.toFile());
                processorContext.writeMainClass();
                processorContext.writeReflectionAutoFeature();
            } finally {
                for (ApplicationArchive archive : context.getAllApplicationArchives()) {
                    try {
                        archive.close();
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Failed to close archive " + archive.getArchiveRoot(), e);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);

        }
    }


    private final class ProcessorContextImpl implements ProcessorContext {


        private final List<DeploymentTaskHolder> tasks = new ArrayList<>();
        private final List<DeploymentTaskHolder> staticInitTasks = new ArrayList<>();
        private final Map<String, ReflectionInfo> reflectiveClasses = new LinkedHashMap<>();
        private final Set<DotName> processedReflectiveHierarchies = new HashSet<>();
        private final Set<String> resources = new HashSet<>();
        private final Set<String> resourceBundles = new HashSet<>();
        private final Set<String> runtimeInitializedClasses = new HashSet<>();
        private final Set<List<String>> proxyClasses = new HashSet<>();
        private final Map<String, Object> properties = new HashMap<>();
        private final Map<String, String> systemProperties = new HashMap<>();
        private final ArchiveContextImpl archiveContext;

        private ProcessorContextImpl(ArchiveContextImpl archiveContext) {
            this.archiveContext = archiveContext;
        }

        @Override
        public BytecodeRecorder addStaticInitTask(int priority) {
            String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
            staticInitTasks.add(new DeploymentTaskHolder(className, priority));
            return new BytecodeRecorderImpl(classLoader, className, StartupTask.class, output);
        }

        @Override
        public BytecodeRecorder addDeploymentTask(int priority) {
            String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
            tasks.add(new DeploymentTaskHolder(className, priority));
            return new BytecodeRecorderImpl(classLoader, className, StartupTask.class, output);
        }

        @Override
        public void addReflectiveClass(boolean method, boolean fields, String... className) {
            for (String cl : className) {
                ReflectionInfo existing = reflectiveClasses.get(cl);
                if (existing == null) {
                    reflectiveClasses.put(cl, new ReflectionInfo(true, method, fields));
                } else {
                    existing.constructors = true;
                    if (method) {
                        existing.methods = true;
                    }
                    if (fields) {
                        existing.fields = true;
                    }
                }
            }
        }

        @Override
        public void addReflectiveField(FieldInfo fieldInfo) {
            String cl = fieldInfo.declaringClass().name().toString();
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false));
            }
            existing.fieldSet.add(fieldInfo.name());
        }

        @Override
        public void addReflectiveField(Field fieldInfo) {
            String cl = fieldInfo.getDeclaringClass().getName();
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false));
            }
            existing.fieldSet.add(fieldInfo.getName());
        }

        @Override
        public void addReflectiveMethod(MethodInfo methodInfo) {
            String cl = methodInfo.declaringClass().name().toString();
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false));
            }
            if (methodInfo.name().equals("<init>")) {
                existing.ctorSet.add(methodInfo);
            } else {
                String[] params = new String[methodInfo.parameters().size()];
                for (int i = 0; i < params.length; ++i) {
                    params[i] = methodInfo.parameters().get(i).name().toString();
                }
                existing.methodSet.add(new MethodData(methodInfo.name(), params));
            }
        }

        @Override
        public void addReflectiveMethod(Method methodInfo) {
            String cl = methodInfo.getDeclaringClass().getName();
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false));
            }
            String[] params = new String[methodInfo.getParameterTypes().length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = methodInfo.getParameterTypes()[i].getName();
            }
            existing.methodSet.add(new MethodData(methodInfo.getName(), params));

        }

        @Override
        public void addReflectiveHierarchy(Type type) {

            if (type instanceof VoidType ||
                    type instanceof PrimitiveType ||
                    type instanceof UnresolvedTypeVariable) {
                return;
            } else if (type instanceof ClassType) {
                addClassTypeHierarchy(type.name());
            } else if (type instanceof ArrayType) {
                addReflectiveHierarchy(type.asArrayType().component());
            } else if (type instanceof ParameterizedType) {
                ParameterizedType p = (ParameterizedType) type;
                addReflectiveHierarchy(p.owner());
                for (Type arg : p.arguments()) {
                    addReflectiveHierarchy(arg);
                }
            }
        }

        private void addClassTypeHierarchy(DotName name) {
            if (name.toString().startsWith("java.") ||
                    processedReflectiveHierarchies.contains(name)) {
                return;
            }
            processedReflectiveHierarchies.add(name);
            addReflectiveClass(true, true, name.toString());
            ClassInfo info = archiveContext.getCombinedIndex().getClassByName(name);
            if (info == null) {
                log.warning("Unable to find annotation info for " + name + ", it may not be correctly registered for reflection");
            } else {
                addClassTypeHierarchy(info.superName());
                for (FieldInfo i : info.fields()) {
                    addReflectiveHierarchy(i.type());
                }
                for (MethodInfo i : info.methods()) {
                    addReflectiveHierarchy(i.returnType());
                    for (Type p : i.parameters()) {
                        addReflectiveHierarchy(p);
                    }
                }
            }

        }


        @Override
        public void addGeneratedClass(boolean applicationClass, String name, byte[] classData) throws IOException {
            output.writeClass(applicationClass, name, classData);
        }

        @Override
        public void createResource(String name, byte[] data) throws IOException {
            output.writeResource(name, data);
        }

        @Override
        public void addByteCodeTransformer(String className, BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
            byteCodeTransformers.computeIfAbsent(className, (e) -> new ArrayList<>()).add(visitorFunction);
        }

        @Override
        public void addResource(String name) {
            resources.add(name);
        }

        @Override
        public void addResourceBundle(String bundle) {
            resourceBundles.add(bundle);
        }

        @Override
        public void addRuntimeInitializedClasses(String... classes) {
            runtimeInitializedClasses.addAll(Arrays.asList(classes));
        }

        @Override
        public void addProxyDefinition(String... proxyClasses) {
            this.proxyClasses.add(Arrays.asList(proxyClasses));
        }

        @Override
        public void addNativeImageSystemProperty(final String name, final String value) {
            systemProperties.put(name, value);
        }

        @Override
        public boolean isCapabilityPresent(String capability) {
            return capabilities.contains(capability);
        }

        @Override
        public <T> void setProperty(String key, T value) {
            properties.put(key, value);
        }

        @Override
        public <T> T getProperty(String key) {
            return (T) properties.get(key);
        }

        void writeProperties(File output) throws IOException {
            final Properties properties = new Properties();
            properties.putAll(systemProperties);
            try (FileOutputStream os = new FileOutputStream(new File(output, "native-image.properties"))) {
                try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                    properties.store(osw, "Generated properties (do not edit)");
                }
            }
        }

        void writeMainClass() throws IOException {

            Collections.sort(tasks);
            if (!useStaticInit) {
                Collections.sort(staticInitTasks);
                tasks.addAll(0, staticInitTasks);
                staticInitTasks.clear();
            } else {
                Collections.sort(staticInitTasks);
            }

            ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(output, true), MAIN_CLASS, null, Object.class.getName());

            FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT, StartupContext.class);
            scField.setModifiers(Modifier.STATIC);

            MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
            mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            mv.invokeStaticMethod(MethodDescriptor.ofMethod(Timing.class, "staticInitStarted", void.class));
            ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
            mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
            TryBlock catchBlock = mv.tryBlock();
            for (DeploymentTaskHolder holder : staticInitTasks) {
                ResultHandle dup = catchBlock.newInstance(ofConstructor(holder.className));
                catchBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
            catchBlock.returnValue(null);

            CatchBlockCreator cb = catchBlock.addCatch(Throwable.class);
            cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
            cb.throwException(RuntimeException.class, "Failed to start shamrock", cb.getCaughtException());

            mv = file.getMethodCreator("main", void.class, String[].class);
            mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            mv.invokeStaticMethod(ofMethod(Timing.class, "mainStarted", void.class));
            startupContext = mv.readStaticField(scField.getFieldDescriptor());
            catchBlock = mv.tryBlock();
            for (DeploymentTaskHolder holder : tasks) {
                ResultHandle dup = catchBlock.newInstance(ofConstructor(holder.className));
                catchBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
            catchBlock.invokeStaticMethod(ofMethod(Timing.class, "printStartupTime", void.class));
            mv.returnValue(null);

            cb = catchBlock.addCatch(Throwable.class);
            cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
            cb.throwException(RuntimeException.class, "Failed to start shamrock", cb.getCaughtException());

            mv = file.getMethodCreator("close", void.class);
            mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            startupContext = mv.readStaticField(scField.getFieldDescriptor());
            mv.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
            mv.returnValue(null);
            file.close();
        }

        void writeReflectionAutoFeature() throws IOException {

            ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(output, true), GRAAL_AUTOFEATURE, null, Object.class.getName(), "org/graalvm/nativeimage/Feature");
            file.addAnnotation("com/oracle/svm/core/annotate/AutomaticFeature");

            //MethodCreator afterReg = file.getMethodCreator("afterRegistration", void.class, "org.graalvm.nativeimage.Feature$AfterRegistrationAccess");
            MethodCreator beforeAn = file.getMethodCreator("beforeAnalysis", "V", "org/graalvm/nativeimage/Feature$BeforeAnalysisAccess");
            TryBlock overallCatch = beforeAn.tryBlock();
            //TODO: at some point we are going to need to break this up, as if it get too big it will hit the method size limit

            if (!runtimeInitializedClasses.isEmpty()) {
                ResultHandle array = overallCatch.newArray(Class.class, overallCatch.load(1));
                ResultHandle thisClass = overallCatch.loadClass(GRAAL_AUTOFEATURE);
                ResultHandle cl = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class), thisClass);
                for (String i : runtimeInitializedClasses) {
                    TryBlock tc = overallCatch.tryBlock();
                    ResultHandle clazz = tc.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class), tc.load(i), tc.load(false), cl);
                    tc.writeArrayValue(array, 0, clazz);
                    tc.invokeStaticMethod(MethodDescriptor.ofMethod("org.graalvm.nativeimage.RuntimeClassInitialization", "delayClassInitialization", void.class, Class[].class), array);

                    CatchBlockCreator cc = tc.addCatch(Throwable.class);
                    cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
                }

            }

            // hack in reinitialization of process info classes
            {
                ResultHandle array = overallCatch.newArray(Class.class, overallCatch.load(1));
                ResultHandle thisClass = overallCatch.loadClass(GRAAL_AUTOFEATURE);
                ResultHandle cl = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getClassLoader", ClassLoader.class), thisClass);
                {
                    TryBlock tc = overallCatch.tryBlock();
                    ResultHandle clazz = tc.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class), tc.load("org.wildfly.common.net.HostName"), tc.load(false), cl);
                    tc.writeArrayValue(array, 0, clazz);
                    tc.invokeStaticMethod(MethodDescriptor.ofMethod("org.graalvm.nativeimage.RuntimeClassInitialization", "rerunClassInitialization", void.class, Class[].class), array);

                    CatchBlockCreator cc = tc.addCatch(Throwable.class);
                    cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
                }
                {
                    TryBlock tc = overallCatch.tryBlock();
                    ResultHandle clazz = tc.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class), tc.load("org.wildfly.common.os.Process"), tc.load(false), cl);
                    tc.writeArrayValue(array, 0, clazz);
                    tc.invokeStaticMethod(MethodDescriptor.ofMethod("org.graalvm.nativeimage.RuntimeClassInitialization", "rerunClassInitialization", void.class, Class[].class), array);

                    CatchBlockCreator cc = tc.addCatch(Throwable.class);
                    cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
                }
            }

            if (!proxyClasses.isEmpty()) {
                ResultHandle proxySupportClass = overallCatch.loadClass("com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry");
                ResultHandle proxySupport = overallCatch.invokeStaticMethod(ofMethod("org.graalvm.nativeimage.ImageSingletons", "lookup", Object.class, Class.class), proxySupportClass);
                for (List<String> proxy : proxyClasses) {
                    ResultHandle array = overallCatch.newArray(Class.class, overallCatch.load(proxy.size()));
                    int i = 0;
                    for (String p : proxy) {
                        ResultHandle clazz = overallCatch.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class), overallCatch.load(p));
                        overallCatch.writeArrayValue(array, i++, clazz);

                    }
                    overallCatch.invokeInterfaceMethod(ofMethod("com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry", "addProxyClass", void.class, Class[].class), proxySupport, array);
                }
            }

            for (String i : resources) {
                overallCatch.invokeStaticMethod(ofMethod(ResourceHelper.class, "registerResources", void.class, String.class), overallCatch.load(i));
            }
            if (!resourceBundles.isEmpty()) {
                ResultHandle locClass = overallCatch.loadClass("com.oracle.svm.core.jdk.LocalizationSupport");

                ResultHandle params = overallCatch.marshalAsArray(Class.class, overallCatch.loadClass(String.class));
                ResultHandle registerMethod = overallCatch.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), locClass, overallCatch.load("addBundleToCache"), params);
                overallCatch.invokeVirtualMethod(ofMethod(AccessibleObject.class, "setAccessible", void.class, boolean.class), registerMethod, overallCatch.load(true));

                ResultHandle locSupport = overallCatch.invokeStaticMethod(MethodDescriptor.ofMethod("org.graalvm.nativeimage.ImageSingletons", "lookup", Object.class, Class.class), locClass);
                for (String i : resourceBundles) {
                    TryBlock et = overallCatch.tryBlock();

                    et.invokeVirtualMethod(ofMethod(Method.class, "invoke", Object.class, Object.class, Object[].class), registerMethod, locSupport, et.marshalAsArray(Object.class, et.load(i)));
                    CatchBlockCreator c = et.addCatch(Throwable.class);
                    //c.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), c.getCaughtException());
                }
            }
            int count = 0;

            for (Map.Entry<String, ReflectionInfo> entry : reflectiveClasses.entrySet()) {

                MethodCreator mv = file.getMethodCreator("registerClass" + count++, "V");
                mv.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
                overallCatch.invokeStaticMethod(mv.getMethodDescriptor());

                TryBlock tc = mv.tryBlock();


                ResultHandle clazz = tc.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class), tc.load(entry.getKey()));
                //we call these methods first, so if they are going to throw an exception it happens before anything has been registered
                ResultHandle constructors = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredConstructors", Constructor[].class), clazz);
                ResultHandle methods = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredMethods", Method[].class), clazz);
                ResultHandle fields = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredFields", Field[].class), clazz);


                ResultHandle carray = tc.newArray(Class.class, tc.load(1));
                tc.writeArrayValue(carray, 0, clazz);
                tc.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Class[].class), carray);


                if (entry.getValue().constructors) {
                    tc.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Executable[].class), constructors);
                } else if (!entry.getValue().ctorSet.isEmpty()) {
                    ResultHandle farray = tc.newArray(Constructor.class, tc.load(1));
                    for (MethodInfo ctor : entry.getValue().ctorSet) {
                        ResultHandle paramArray = tc.newArray(Class.class, tc.load(ctor.parameters().size()));
                        for (int i = 0; i < ctor.parameters().size(); ++i) {
                            Type type = ctor.parameters().get(i);
                            tc.writeArrayValue(paramArray, i, tc.loadClass(type.name().toString()));
                        }
                        ResultHandle fhandle = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredConstructor", Constructor.class, Class[].class), clazz, paramArray);
                        tc.writeArrayValue(farray, 0, fhandle);
                        tc.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Executable[].class), farray);
                    }
                }
                if (entry.getValue().methods) {
                    tc.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Executable[].class), methods);
                } else if (!entry.getValue().methodSet.isEmpty()) {
                    ResultHandle farray = tc.newArray(Method.class, tc.load(1));
                    for (MethodData method : entry.getValue().methodSet) {
                        ResultHandle paramArray = tc.newArray(Class.class, tc.load(method.params.length));
                        for (int i = 0; i < method.params.length; ++i) {
                            String type = method.params[i];
                            tc.writeArrayValue(paramArray, i, tc.loadClass(type));
                        }
                        ResultHandle fhandle = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), clazz, tc.load(method.name), paramArray);
                        tc.writeArrayValue(farray, 0, fhandle);
                        tc.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Executable[].class), farray);
                    }
                }
                if (entry.getValue().fields) {
                    tc.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Field[].class), fields);
                } else if (!entry.getValue().fieldSet.isEmpty()) {
                    ResultHandle farray = tc.newArray(Field.class, tc.load(1));
                    for (String field : entry.getValue().fieldSet) {
                        ResultHandle fhandle = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredField", Field.class, String.class), clazz, tc.load(field));
                        tc.writeArrayValue(farray, 0, fhandle);
                        tc.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Field[].class), farray);
                    }
                }
                CatchBlockCreator cc = tc.addCatch(Throwable.class);
                //cc.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cc.getCaughtException());
                mv.returnValue(null);
            }
            CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
            print.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), print.getCaughtException());

            beforeAn.returnValue(null);

            file.close();
        }

    }

    private static final class DeploymentTaskHolder implements Comparable<DeploymentTaskHolder> {
        private final String className;
        private final int priority;

        private DeploymentTaskHolder(String className, int priority) {
            this.className = className;
            this.priority = priority;
        }

        @Override
        public int compareTo(DeploymentTaskHolder o) {
            int val = Integer.compare(priority, o.priority);
            if (val != 0) {
                return val;
            }
            return className.compareTo(o.className);
        }
    }

    static final class MethodData {

        final String name;
        final String[] params;

        MethodData(String name, String[] params) {
            this.name = name;
            this.params = params;
        }
    }

    static final class ReflectionInfo {
        boolean constructors;
        boolean methods;
        boolean fields;
        Set<String> fieldSet = new HashSet<>();
        Set<MethodData> methodSet = new HashSet<>();
        Set<MethodInfo> ctorSet = new HashSet<>();

        private ReflectionInfo(boolean constructors, boolean methods, boolean fields) {
            this.methods = methods;
            this.fields = fields;
            this.constructors = constructors;
        }
    }

    static final class HierachyInfo {
        boolean methods;
        boolean fields;
        final Type type;

        HierachyInfo(Type type, boolean methods, boolean fields) {
            this.type = type;
            this.methods = methods;
            this.fields = fields;
        }

        public boolean isMethods() {
            return methods;
        }

        public void setMethods(boolean methods) {
            this.methods = methods;
        }

        public boolean isFields() {
            return fields;
        }

        public void setFields(boolean fields) {
            this.fields = fields;
        }

        public Type getType() {
            return type;
        }
    }
}
