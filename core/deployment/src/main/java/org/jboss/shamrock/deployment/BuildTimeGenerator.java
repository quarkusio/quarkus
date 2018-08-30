package org.jboss.shamrock.deployment;

import static org.jboss.protean.gizmo.MethodDescriptor.ofConstructor;
import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ExceptionTable;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
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
    public static final String MAIN_CLASS = MAIN_CLASS_INTERNAL.replace("/", ".");
    private static final String GRAAL_AUTOFEATURE = "org/jboss/shamrock/runner/AutoFeature";
    private static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";

    private final List<ResourceProcessor> processors;
    private final ClassOutput output;
    private final DeploymentProcessorInjection injection;
    private final ClassLoader classLoader;
    private final boolean useStaticInit;
    private final List<Function<String, Function<ClassVisitor, ClassVisitor>>> bytecodeTransformers = new ArrayList<>();
    private final Set<String> applicationArchiveMarkers;

    public BuildTimeGenerator(ClassOutput classOutput, ClassLoader cl, boolean useStaticInit) {
        this.useStaticInit = useStaticInit;
        Iterator<ShamrockSetup> loader = ServiceLoader.load(ShamrockSetup.class, cl).iterator();
        SetupContextImpl setupContext = new SetupContextImpl();
        while (loader.hasNext()) {
            loader.next().setup(setupContext);
        }
        setupContext.resourceProcessors.sort(Comparator.comparingInt(ResourceProcessor::getPriority));
        this.processors = Collections.unmodifiableList(setupContext.resourceProcessors);
        this.output = classOutput;
        this.injection = new DeploymentProcessorInjection(setupContext.injectionProviders);
        this.classLoader = cl;
        this.applicationArchiveMarkers = new HashSet<>(setupContext.applicationArchiveMarkers);
    }

    public List<Function<String, Function<ClassVisitor, ClassVisitor>>> getBytecodeTransformers() {
        return bytecodeTransformers;
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
            List<ApplicationArchive> applicationArchives = ApplicationArchiveLoader.scanForOtherIndexes(classLoader, config, applicationArchiveMarkers, root);


            ArchiveContext context = new ArchiveContextImpl(new ApplicationArchiveImpl(appIndex, root, null), applicationArchives, config);
            ProcessorContextImpl processorContext = new ProcessorContextImpl();
            try {
                for (ResourceProcessor processor : processors) {
                    try {
                        injection.injectClass(processor);
                        processor.process(context, processorContext);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
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
        private final Set<String> resources = new HashSet<>();

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
                    reflectiveClasses.put(cl, new ReflectionInfo(method, fields));
                } else {
                    reflectiveClasses.put(cl, new ReflectionInfo(method || existing.methods, fields || existing.fields));
                }
            }
        }

        @Override
        public void addGeneratedClass(boolean applicationClass, String name, byte[] classData) throws IOException {
            output.writeClass(applicationClass, name, classData);
        }

        @Override
        public void addByteCodeTransformer(Function<String, Function<ClassVisitor, ClassVisitor>> visitorFunction) {
            bytecodeTransformers.add(visitorFunction);
        }

        @Override
        public void addResource(String name) {
            resources.add(name);
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
            for (DeploymentTaskHolder holder : staticInitTasks) {
                ResultHandle dup = mv.newInstance(ofConstructor(holder.className));
                mv.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
            mv.returnValue(null);

            mv = file.getMethodCreator("main", void.class, String[].class);
            mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            mv.invokeStaticMethod(ofMethod(Timing.class, "mainStarted", void.class));
            startupContext = mv.readStaticField(scField.getFieldDescriptor());

            for (DeploymentTaskHolder holder : tasks) {
                ResultHandle dup = mv.newInstance(ofConstructor(holder.className));
                mv.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
            mv.returnValue(null);

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


            MethodCreator mv = file.getMethodCreator("beforeAnalysis", "V", "org/graalvm/nativeimage/Feature$BeforeAnalysisAccess");

            //TODO: at some point we are going to need to break this up, as if it get too big it will hit the method size limit

            for (String i : resources) {
                mv.invokeStaticMethod(ofMethod(ResourceHelper.class, "registerResources", void.class, String.class), mv.load(i));
            }

            for (Map.Entry<String, ReflectionInfo> entry : reflectiveClasses.entrySet()) {
                ExceptionTable exceptionTable = mv.addTryCatch();
                ResultHandle carray = mv.newArray(Class.class, mv.load(1));
                ResultHandle clazz = mv.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class), mv.load(entry.getKey()));
                mv.writeArrayValue(carray, mv.load(0), clazz);
                mv.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Class[].class), carray);
                //now load constructors
                ResultHandle res = mv.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredConstructors", Constructor[].class), clazz);
                mv.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Executable[].class), res);
                //now load everything else
                if (entry.getValue().methods) {
                    res = mv.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredMethods", Method[].class), clazz);
                    mv.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Executable[].class), res);
                }
                if (entry.getValue().fields) {
                    res = mv.invokeVirtualMethod(ofMethod(Class.class, "getDeclaredFields", Field[].class), clazz);
                    mv.invokeStaticMethod(ofMethod("org/graalvm/nativeimage/RuntimeReflection", "register", void.class, Field[].class), res);
                }
                exceptionTable.addCatchClause(Throwable.class);
                exceptionTable.complete();
            }
            mv.returnValue(null);
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

    static final class ReflectionInfo {
        final boolean methods;
        final boolean fields;

        private ReflectionInfo(boolean methods, boolean fields) {
            this.methods = methods;
            this.fields = fields;
        }
    }
}
