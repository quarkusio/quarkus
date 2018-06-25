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
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.shamrock.codegen.BytecodeRecorder;
import org.jboss.shamrock.startup.DeploymentTask;
import org.jboss.shamrock.startup.StartupContext;


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
        Collections.sort(processors, Comparator.comparingInt(ResourceProcessor::getPriority));
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

        @Override
        public <T> T getAttachment(AttachmentKey<T> key) {
            return null;
        }

        @Override
        public <T> void setAttachment(AttachmentKey<T> key, T value) {

        }

        @Override
        public <T> void addToList(ListAttachmentKey<T> key, T value) {

        }

        @Override
        public <T> List<T> getList(ListAttachmentKey<T> key) {
            return null;
        }
    }


    private final class ProcessorContextImpl implements ProcessorContext {


        private final List<DeploymentTaskHolder> tasks = new ArrayList<>();

        @Override
        public BytecodeRecorder addDeploymentTask(int priority) {
            String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
            tasks.add(new DeploymentTaskHolder(className, priority));
            return new BytecodeRecorder(className, DeploymentTask.class, output);
        }

        void writeMainClass() throws IOException {
            ClassFile file = new ClassFile("org.jboss.shamrock.runner.Main", "java.lang.Object");
            ClassMethod mainMethod = file.addMethod(AccessFlag.PUBLIC | AccessFlag.STATIC, "main", "V", "[Ljava/lang/String;");
            CodeAttribute ca = mainMethod.getCodeAttribute();
            ca.newInstruction(StartupContext.class);
            ca.dup();
            ca.invokespecial(StartupContext.class.getName(), "<init>", "()V");
            for (DeploymentTaskHolder holder : tasks) {
                ca.dup();
                ca.newInstruction(holder.className);
                ca.dup();
                ca.invokespecial(holder.className, "<init>", "()V");
                ca.swap();
                ca.invokeinterface(DeploymentTask.class.getName(), "deploy", "(Lorg/jboss/shamrock/startup/StartupContext;)V");
            }
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
