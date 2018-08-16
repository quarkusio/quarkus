package org.jboss.shamrock.deployment;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SWAP;

import java.io.IOException;
import java.io.InputStream;
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

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorderImpl;
import org.jboss.shamrock.deployment.index.ApplicationArchiveLoader;
import org.jboss.shamrock.runtime.ResourceHelper;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;
import org.jboss.shamrock.runtime.Timing;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

    public BuildTimeGenerator(ClassOutput classOutput, ClassLoader cl, boolean useStaticInit) {
        this.useStaticInit = useStaticInit;
        Iterator<ResourceProcessor> loader = ServiceLoader.load(ResourceProcessor.class, cl).iterator();
        List<ResourceProcessor> processors = new ArrayList<>();
        while (loader.hasNext()) {
            processors.add(loader.next());
        }
        processors.sort(Comparator.comparingInt(ResourceProcessor::getPriority));
        this.processors = Collections.unmodifiableList(processors);
        this.output = classOutput;
        this.injection = new DeploymentProcessorInjection(cl);
        this.classLoader = cl;
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
            List<ApplicationArchive> applicationArchives = ApplicationArchiveLoader.scanForOtherIndexes(classLoader, config);


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
                for(ApplicationArchive archive : context.getAllApplicationArchives()) {
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

            ClassWriter file = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            file.visit(Opcodes.V1_8, ACC_PUBLIC | ACC_SUPER, MAIN_CLASS_INTERNAL, null, Type.getInternalName(Object.class), null);

            // constructor
            MethodVisitor mv = file.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0); // push `this` to the operand stack
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false); // call the constructor of super class
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();

            file.visitField(ACC_PUBLIC | ACC_STATIC, STARTUP_CONTEXT, "L" + Type.getInternalName(StartupContext.class) + ";", null, null);

            mv = file.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Timing.class), "staticInitStarted", "()V", false);
            mv.visitTypeInsn(NEW, Type.getInternalName(StartupContext.class));
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(StartupContext.class), "<init>", "()V", false);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(PUTSTATIC, MAIN_CLASS_INTERNAL, STARTUP_CONTEXT, "L" + Type.getInternalName(StartupContext.class) + ";");
            for (DeploymentTaskHolder holder : staticInitTasks) {
                mv.visitInsn(DUP);
                String className = holder.className.replace(".", "/");
                mv.visitTypeInsn(NEW, className);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(StartupTask.class), "deploy", "(L" + StartupContext.class.getName().replace(".", "/") + ";)V", true);
            }

            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 4);
            mv.visitEnd();

            mv = file.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Timing.class), "mainStarted", "()V", false);
            mv.visitFieldInsn(GETSTATIC, MAIN_CLASS_INTERNAL, STARTUP_CONTEXT, "L" + Type.getInternalName(StartupContext.class) + ";");
            for (DeploymentTaskHolder holder : tasks) {
                mv.visitInsn(DUP);
                String className = holder.className.replace(".", "/");
                mv.visitTypeInsn(NEW, className);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(StartupTask.class), "deploy", "(L" + StartupContext.class.getName().replace(".", "/") + ";)V", true);
            }

            //time since main start
            mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Timing.class), "printStartupTime", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 4);
            mv.visitEnd();


            mv = file.visitMethod(ACC_PUBLIC | ACC_STATIC, "close", "()V", null, null);
            mv.visitFieldInsn(GETSTATIC, MAIN_CLASS_INTERNAL, STARTUP_CONTEXT, "L" + Type.getInternalName(StartupContext.class) + ";");
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(StartupContext.class), "close", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 4);
            mv.visitEnd();

            file.visitEnd();

            output.writeClass(true, MAIN_CLASS_INTERNAL, file.toByteArray());
        }

        void writeReflectionAutoFeature() throws IOException {

            ClassWriter file = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            file.visit(Opcodes.V1_8, ACC_PUBLIC | ACC_SUPER, GRAAL_AUTOFEATURE, null, Type.getInternalName(Object.class), new String[]{"org/graalvm/nativeimage/Feature"});
            AnnotationVisitor annotation = file.visitAnnotation("Lcom/oracle/svm/core/annotate/AutomaticFeature;", true);
            annotation.visitEnd();
            // constructor
            MethodVisitor mv = file.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0); // push `this` to the operand stack
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false); // call the constructor of super class
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();


            mv = file.visitMethod(ACC_PUBLIC, "beforeAnalysis", "(Lorg/graalvm/nativeimage/Feature$BeforeAnalysisAccess;)V", null, null);

            //TODO: at some point we are going to need to break this up, as if it get too big it will hit the method size limit

            for(String i : resources) {
                mv.visitLdcInsn(i);
                mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ResourceHelper.class), "registerResources", "(Ljava/lang/String;)V", false);
            }

            for (Map.Entry<String, ReflectionInfo> entry : reflectiveClasses.entrySet()) {
                Label lTryBlockStart = new Label();
                Label lTryBlockEnd = new Label();
                Label lCatchBlockStart = new Label();
                Label lCatchBlockEnd = new Label();

                // set up try-catch block for RuntimeException
                mv.visitTryCatchBlock(lTryBlockStart, lTryBlockEnd,
                        lCatchBlockStart, "java/lang/Exception");

                // started the try block
                mv.visitLabel(lTryBlockStart);
                mv.visitLdcInsn(1);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
                mv.visitInsn(DUP);
                mv.visitLdcInsn(0);
                mv.visitLdcInsn(entry.getKey());
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                mv.visitInsn(AASTORE);
                mv.visitMethodInsn(INVOKESTATIC, "org/graalvm/nativeimage/RuntimeReflection", "register", "([Ljava/lang/Class;)V", false);


                //now load everything else
                mv.visitLdcInsn(entry.getKey());
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                if (entry.getValue().methods) {
                    mv.visitInsn(DUP);
                }
                if (entry.getValue().fields) {
                    mv.visitInsn(DUP);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructors", "()[Ljava/lang/reflect/Constructor;", false);
                mv.visitMethodInsn(INVOKESTATIC, "org/graalvm/nativeimage/RuntimeReflection", "register", "([Ljava/lang/reflect/Executable;)V", false);
                //now load everything else
                if (entry.getValue().methods) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethods", "()[Ljava/lang/reflect/Method;", false);
                    mv.visitMethodInsn(INVOKESTATIC, "org/graalvm/nativeimage/RuntimeReflection", "register", "([Ljava/lang/reflect/Executable;)V", false);
                }
                if (entry.getValue().fields) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredFields", "()[Ljava/lang/reflect/Field;", false);
                    mv.visitMethodInsn(INVOKESTATIC, "org/graalvm/nativeimage/RuntimeReflection", "register", "([Ljava/lang/reflect/Field;)V", false);
                }
                mv.visitLabel(lTryBlockEnd);
                mv.visitLabel(lCatchBlockStart);
                mv.visitLabel(lCatchBlockEnd);
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 2);
            mv.visitEnd();
            file.visitEnd();

            output.writeClass(true, GRAAL_AUTOFEATURE, file.toByteArray());
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
