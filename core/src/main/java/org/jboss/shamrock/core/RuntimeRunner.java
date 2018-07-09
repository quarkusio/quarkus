package org.jboss.shamrock.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.shamrock.codegen.BytecodeRecorder;
import org.jboss.shamrock.startup.StartupTast;
import org.jboss.shamrock.startup.StartupContext;

/**
 * An entry point that can both process the metadata and then run the resulting output, without generating any bytecode
 */
public class RuntimeRunner {

    private final List<ResourceProcessor> processors;
    private final List<RuntimeTaskHolder> runtimeTasks = new ArrayList<>();

    public static void main(String... args) throws Exception {
        URL uri = RuntimeRunner.class.getResource("RuntimeRunner.class");
        String val = uri.toExternalForm();
        if (val.contains("!")) {
            val = val.substring(0, val.lastIndexOf('!'));
        }
        FileSystem fs = FileSystems.newFileSystem(new URI(val), new HashMap<>());

        new RuntimeRunner().run(fs.getRootDirectories().iterator().next());
    }

    public RuntimeRunner() {
        Iterator<ResourceProcessor> loader = ServiceLoader.load(ResourceProcessor.class).iterator();
        List<ResourceProcessor> processors = new ArrayList<>();
        while (loader.hasNext()) {
            processors.add(loader.next());
        }
        Collections.sort(processors, Comparator.comparingInt(ResourceProcessor::getPriority));
        this.processors = Collections.unmodifiableList(processors);
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
        Collections.sort(runtimeTasks);
        StartupContext sc = new StartupContext();
        for (RuntimeTaskHolder task : runtimeTasks) {
            task.recorder.executeRuntime(sc);
        }
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


        @Override
        public BytecodeRecorder addDeploymentTask(int priority) {
            BytecodeRecorder recorder = new BytecodeRecorder(null, StartupTast.class, null, true);
            runtimeTasks.add(new RuntimeTaskHolder(recorder, priority));
            return recorder;
        }

        @Override
        public void addReflectiveClass(String className) {
        }

        @Override
        public void addGeneratedClass(String name, byte[] classData) throws IOException {
            //TODO
        }


    }

    private static final class RuntimeTaskHolder implements Comparable<RuntimeTaskHolder> {
        private final BytecodeRecorder recorder;
        private final int priority;

        private RuntimeTaskHolder(BytecodeRecorder recorder, int priority) {
            this.recorder = recorder;
            this.priority = priority;
        }

        @Override
        public int compareTo(RuntimeTaskHolder o) {
            int val = Integer.compare(priority, o.priority);
            if (val != 0) {
                return val;
            }
            return recorder.getServiceType().getName().compareTo(o.recorder.getServiceType().getName());
        }
    }
}
