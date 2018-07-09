package org.jboss.shamrock.core;

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
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.annotations.ClassAnnotation;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.shamrock.codegen.BytecodeRecorder;
import org.jboss.shamrock.startup.StartupTast;
import org.jboss.shamrock.startup.StartupContext;

/**
 * Class that does the build time processing
 */
public class Runner {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private final List<ResourceProcessor> processors;
    private final ClassOutput output;

    public Runner(ClassOutput classOutput) {
        Iterator<ResourceProcessor> loader = ServiceLoader.load(ResourceProcessor.class).iterator();
        List<ResourceProcessor> processors = new ArrayList<>();
        while (loader.hasNext()) {
            processors.add(loader.next());
        }
        processors.sort(Comparator.comparingInt(ResourceProcessor::getPriority));
        this.processors = Collections.unmodifiableList(processors);
        this.output = classOutput;
    }


    public void run(Path root) throws IOException {
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
        Index index = indexer.complete();
        ArchiveContext context = new ArchiveContextImpl(index, root);
        ProcessorContextImpl processorContext = new ProcessorContextImpl();
        for (ResourceProcessor processor : processors) {
            try {
                processor.process(context, processorContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        processorContext.writeMainClass();
        processorContext.writeAutoFeature();
    }


    private static class ArchiveContextImpl implements ArchiveContext {

        private final Index index;
        private final Path root;

        private ArchiveContextImpl(Index index, Path root) {
            this.index = index;
            this.root = root;
        }

        @Override
        public Index getIndex() {
            return index;
        }

        @Override
        public Path getArchiveRoot() {
            return root;
        }
    }


    private final class ProcessorContextImpl implements ProcessorContext {


        private final List<DeploymentTaskHolder> tasks = new ArrayList<>();
        private final List<String> reflectiveClasses = new ArrayList<>();

        @Override
        public BytecodeRecorder addDeploymentTask(int priority) {
            String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
            tasks.add(new DeploymentTaskHolder(className, priority));
            return new BytecodeRecorder(className, StartupTast.class, output, false);
        }

        @Override
        public void addReflectiveClass(String className) {
            reflectiveClasses.add(className);
        }

        @Override
        public void addGeneratedClass(String name, byte[] classData) throws IOException {
            output.writeClass(name, classData);
        }

        void writeMainClass() throws IOException {
            ClassFile file = new ClassFile("org.jboss.shamrock.runner.Main", "java.lang.Object");
            ClassMethod mainMethod = file.addMethod(AccessFlag.PUBLIC | AccessFlag.STATIC, "main", "V", "[Ljava/lang/String;");
            CodeAttribute ca = mainMethod.getCodeAttribute();
            ca.newInstruction(StartupContext.class);
            ca.dup();
            ca.invokespecial(StartupContext.class.getName(), "<init>", "()V");
            Collections.sort(tasks);
            for (DeploymentTaskHolder holder : tasks) {
                ca.dup();
                ca.newInstruction(holder.className);
                ca.dup();
                ca.invokespecial(holder.className, "<init>", "()V");
                ca.swap();
                ca.invokeinterface(StartupTast.class.getName(), "deploy", "(Lorg/jboss/shamrock/startup/StartupContext;)V");
            }
            ca.returnInstruction();
            output.writeClass(file.getName(), file.toBytecode());
        }

        void writeAutoFeature() throws IOException {

            ClassFile file = new ClassFile("org.jboss.shamrock.runner.AutoFeature", "java.lang.Object", "org.graalvm.nativeimage.Feature");
            ClassMethod mainMethod = file.addMethod(AccessFlag.PUBLIC, "beforeAnalysis", "V", "Lorg/graalvm/nativeimage/Feature$BeforeAnalysisAccess;");
            file.getRuntimeVisibleAnnotationsAttribute().addAnnotation(new ClassAnnotation(file.getConstPool(), "com.oracle.svm.core.annotate.AutomaticFeature", Collections.emptyList()));

            CodeAttribute ca = mainMethod.getCodeAttribute();

            for (String holder : reflectiveClasses) {
                ca.ldc(1);
                ca.anewarray("java/lang/Class");
                ca.dup();
                ca.ldc(0);
                ca.loadClass(holder);
                ca.aastore();
                ca.invokestatic("org.graalvm.nativeimage.RuntimeReflection", "register", "([Ljava/lang/Class;)V");

                //now load everything else
                ca.loadClass(holder);
                ca.invokevirtual("java.lang.Class", "getDeclaredConstructors", "()[Ljava/lang/reflect/Constructor;");
                ca.invokestatic("org.graalvm.nativeimage.RuntimeReflection", "register", "([Ljava/lang/reflect/Executable;)V");
                //now load everything else
                ca.loadClass(holder);
                ca.invokevirtual("java.lang.Class", "getMethods", "()[Ljava/lang/reflect/Method;");
                ca.invokestatic("org.graalvm.nativeimage.RuntimeReflection", "register", "([Ljava/lang/reflect/Executable;)V");

            }
            ca.returnInstruction();

            ClassMethod ctor = file.addMethod(AccessFlag.PUBLIC, "<init>", "V");
            ca = ctor.getCodeAttribute();
            ca.aload(0);
            ca.invokespecial(Object.class.getName(), "<init>", "()V");
            ca.returnInstruction();
            output.writeClass(file.getName(), file.toBytecode());
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
}
