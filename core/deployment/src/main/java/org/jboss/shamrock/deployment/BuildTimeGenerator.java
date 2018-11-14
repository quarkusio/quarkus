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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.builder.BuildChain;
import org.jboss.builder.BuildContext;
import org.jboss.builder.BuildResult;
import org.jboss.builder.BuildStep;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.protean.gizmo.CatchBlockCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.protean.gizmo.TryBlock;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.ArchiveRootBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.LogSetupBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveFieldBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import org.jboss.shamrock.deployment.index.ApplicationArchiveLoader;
import org.jboss.shamrock.deployment.recording.BytecodeRecorderImpl;
import org.jboss.shamrock.deployment.recording.MainBytecodeRecorderBuildItem;
import org.jboss.shamrock.deployment.recording.StaticBytecodeRecorderBuildItem;
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
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final ClassOutput output;
    private final ClassLoader classLoader;
    private final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> byteCodeTransformers = new HashMap<>();
    private final ArchiveContextBuilder archiveContextBuilder;

    public BuildTimeGenerator(ClassOutput classOutput, ClassLoader cl, ArchiveContextBuilder contextBuilder) {
        this.output = classOutput;
        this.classLoader = cl;
        this.archiveContextBuilder = contextBuilder;
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
            List<ApplicationArchive> applicationArchives = ApplicationArchiveLoader.scanForOtherIndexes(classLoader, config, Collections.emptySet(), root, archiveContextBuilder.getAdditionalApplicationArchives());

            ArchiveContextImpl archiveContext = new ArchiveContextImpl(new ApplicationArchiveImpl(appIndex, root, null), applicationArchives, config);

            ProcessorContextImpl processorContext = new ProcessorContextImpl();
            processorContext.addResource("META-INF/microprofile-config.properties");


            try {

                BuildChain chain = BuildChain.builder()

                        .loadProviders(Thread.currentThread().getContextClassLoader())
                        .addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                context.produce(ShamrockConfig.INSTANCE);
                                context.produce(new ApplicationArchivesBuildItem(archiveContext));
                                context.produce(new CombinedIndexBuildItem(archiveContext.getCombinedIndex()));
                                context.produce(new ArchiveRootBuildItem(archiveContext.getRootArchive().getArchiveRoot()));
                                context.produce(archiveContext.getBuildConfig());
                            }
                        })
                        .produces(ShamrockConfig.class)
                        .produces(ApplicationArchivesBuildItem.class)
                        .produces(CombinedIndexBuildItem.class)
                        .produces(ArchiveRootBuildItem.class)
                        .produces(BuildConfig.class)
                        .consumes(LogSetupBuildItem.class)
                        .build()
                        .addFinal(ReflectiveClassBuildItem.class)
                        .addFinal(RuntimeInitializedClassBuildItem.class)
                        .addFinal(GeneratedClassBuildItem.class)
                        .addFinal(GeneratedResourceBuildItem.class)
                        .addFinal(BytecodeTransformerBuildItem.class)
                        .addFinal(SubstrateResourceBuildItem.class)
                        .addFinal(SubstrateResourceBundleBuildItem.class)
                        .addFinal(ReflectiveFieldBuildItem.class)
                        .addFinal(ReflectiveMethodBuildItem.class)
                        .addFinal(StaticBytecodeRecorderBuildItem.class)
                        .addFinal(MainBytecodeRecorderBuildItem.class)
                        .addFinal(SubstrateSystemPropertyBuildItem.class)
                        .build();
                BuildResult result = chain.createExecutionBuilder("main").execute();

                for (GeneratedClassBuildItem i : result.consumeMulti(GeneratedClassBuildItem.class)) {
                    processorContext.addGeneratedClass(i.isApplicationClass(), i.getName(), i.getClassData());
                }
                for (GeneratedResourceBuildItem i : result.consumeMulti(GeneratedResourceBuildItem.class)) {
                    processorContext.createResource(i.getName(), i.getClassData());
                }
                for (BytecodeTransformerBuildItem i : result.consumeMulti(BytecodeTransformerBuildItem.class)) {
                    processorContext.addByteCodeTransformer(i.getClassToTransform(), i.getVisitorFunction());
                }
                for (RuntimeInitializedClassBuildItem i : result.consumeMulti(RuntimeInitializedClassBuildItem.class)) {
                    processorContext.addRuntimeInitializedClasses(i.getClassName());
                }
                for (SubstrateResourceBuildItem i : result.consumeMulti(SubstrateResourceBuildItem.class)) {
                    for(String j : i.getResources()) {
                        processorContext.addResource(j);
                    }
                }
                for (SubstrateResourceBundleBuildItem i : result.consumeMulti(SubstrateResourceBundleBuildItem.class)) {
                    processorContext.addResourceBundle(i.getBundleName());
                }
                for (ReflectiveClassBuildItem i : result.consumeMulti(ReflectiveClassBuildItem.class)) {
                    processorContext.addReflectiveClass(i.isMethods(), i.isFields(), i.getClassNames().toArray(EMPTY_STRING_ARRAY));
                }
                for (SubstrateProxyDefinitionBuildItem i : result.consumeMulti(SubstrateProxyDefinitionBuildItem.class)) {
                    processorContext.addProxyDefinition(i.getClasses().toArray(EMPTY_STRING_ARRAY));
                }
                for (ReflectiveMethodBuildItem i : result.consumeMulti(ReflectiveMethodBuildItem.class)) {
                    processorContext.addReflectiveMethod(i);
                }
                for (ReflectiveFieldBuildItem i : result.consumeMulti(ReflectiveFieldBuildItem.class)) {
                    processorContext.addReflectiveField(i);
                }
                for (MainBytecodeRecorderBuildItem i : result.consumeMulti(MainBytecodeRecorderBuildItem.class)) {
                    processorContext.addDeploymentTask(i.getBytecodeRecorder());
                }
                for (StaticBytecodeRecorderBuildItem i : result.consumeMulti(StaticBytecodeRecorderBuildItem.class)) {
                    processorContext.addStaticInitTask(i.getBytecodeRecorder());
                }
                for (SubstrateSystemPropertyBuildItem i : result.consumeMulti(SubstrateSystemPropertyBuildItem.class)) {
                    processorContext.addNativeImageSystemProperty(i.getKey(), i.getValue());
                }

                processorContext.writeProperties(root.toFile());
                processorContext.writeMainClass();
                processorContext.writeReflectionAutoFeature();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                for (ApplicationArchive archive : archiveContext.getAllApplicationArchives()) {
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


    private final class ProcessorContextImpl {


        private final List<BytecodeRecorderImpl> tasks = new CopyOnWriteArrayList<>();
        private final List<BytecodeRecorderImpl> staticInitTasks = new CopyOnWriteArrayList<>();
        private final Map<String, ReflectionInfo> reflectiveClasses = new LinkedHashMap<>();
        private final Set<String> resources = new HashSet<>();
        private final Set<String> resourceBundles = new HashSet<>();
        private final Set<String> runtimeInitializedClasses = new HashSet<>();
        private final Set<List<String>> proxyClasses = new HashSet<>();
        private final Map<String, String> systemProperties = new HashMap<>();

        public void addStaticInitTask(BytecodeRecorderImpl recorder) {
            staticInitTasks.add(recorder);
        }

        public void addDeploymentTask(BytecodeRecorderImpl recorder) {
            tasks.add(recorder);
        }

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

        public void addReflectiveField(ReflectiveFieldBuildItem fieldInfo) {
            String cl = fieldInfo.getDeclaringClass();
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false));
            }
            existing.fieldSet.add(fieldInfo.getName());
        }

        public void addReflectiveMethod(ReflectiveMethodBuildItem methodInfo) {
            String cl = methodInfo.getDeclaringClass();
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                reflectiveClasses.put(cl, existing = new ReflectionInfo(false, false, false));
            }
            if (methodInfo.getName().equals("<init>")) {
                existing.ctorSet.add(methodInfo);
            } else {
                existing.methodSet.add(methodInfo);
            }
        }

        public void addGeneratedClass(boolean applicationClass, String name, byte[] classData) throws IOException {
            output.writeClass(applicationClass, name, classData);
        }

        public void createResource(String name, byte[] data) throws IOException {
            output.writeResource(name, data);
        }

        public void addByteCodeTransformer(String className, BiFunction<String, ClassVisitor, ClassVisitor> visitorFunction) {
            byteCodeTransformers.computeIfAbsent(className, (e) -> new ArrayList<>()).add(visitorFunction);
        }

        public void addResource(String name) {
            resources.add(name);
        }

        public void addResourceBundle(String bundle) {
            resourceBundles.add(bundle);
        }

        public void addRuntimeInitializedClasses(String... classes) {
            runtimeInitializedClasses.addAll(Arrays.asList(classes));
        }

        public void addProxyDefinition(String... proxyClasses) {
            this.proxyClasses.add(Arrays.asList(proxyClasses));
        }

        public void addNativeImageSystemProperty(final String name, final String value) {
            systemProperties.put(name, value);
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

            ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(output, true), MAIN_CLASS, null, Object.class.getName());

            FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT, StartupContext.class);
            scField.setModifiers(Modifier.STATIC);

            MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
            mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            mv.invokeStaticMethod(MethodDescriptor.ofMethod(Timing.class, "staticInitStarted", void.class));
            ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
            mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
            TryBlock catchBlock = mv.tryBlock();
            for (BytecodeRecorderImpl holder : staticInitTasks) {
                if (!holder.isEmpty()) {
                    String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
                    holder.writeBytecode(output, className);

                    ResultHandle dup = catchBlock.newInstance(ofConstructor(className));
                    catchBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
                }
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
            for (BytecodeRecorderImpl holder : tasks) {
                if (!holder.isEmpty()) {
                    String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
                    holder.writeBytecode(output, className);
                    ResultHandle dup = catchBlock.newInstance(ofConstructor(className));
                    catchBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
                }
            }
            catchBlock.invokeStaticMethod(ofMethod(Timing.class, "printStartupTime", void.class));
            mv.returnValue(null);

            cb = catchBlock.addCatch(Throwable.class);
            cb.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cb.getCaughtException());
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
                    for (ReflectiveMethodBuildItem ctor : entry.getValue().ctorSet) {
                        ResultHandle paramArray = tc.newArray(Class.class, tc.load(ctor.getParams().length));
                        for (int i = 0; i < ctor.getParams().length; ++i) {
                            String type = ctor.getParams()[i];
                            tc.writeArrayValue(paramArray, i, tc.loadClass(type));
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
                    for (ReflectiveMethodBuildItem method : entry.getValue().methodSet) {
                        ResultHandle paramArray = tc.newArray(Class.class, tc.load(method.getParams().length));
                        for (int i = 0; i < method.getParams().length; ++i) {
                            String type = method.getParams()[i];
                            tc.writeArrayValue(paramArray, i, tc.loadClass(type));
                        }
                        ResultHandle fhandle = tc.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredMethod", Method.class, String.class, Class[].class), clazz, tc.load(method.getName()), paramArray);
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

    static final class ReflectionInfo {
        boolean constructors;
        boolean methods;
        boolean fields;
        Set<String> fieldSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> methodSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> ctorSet = new HashSet<>();

        private ReflectionInfo(boolean constructors, boolean methods, boolean fields) {
            this.methods = methods;
            this.fields = fields;
            this.constructors = constructors;
        }
    }

}
