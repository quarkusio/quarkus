package io.quarkus.deployment.steps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.TransformedArchiveBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;

public class ClassTransformingBuildStep {

    private static final Logger log = Logger.getLogger(ClassTransformingBuildStep.class);

    //we specify loadsApplicationClasses=true in case the transformation process attempts to load app classes
    @BuildStep(loadsApplicationClasses = true)
    TransformedClassesBuildItem handleClassTransformation(List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems,
            ApplicationArchivesBuildItem appArchives) throws ExecutionException, InterruptedException {
        if (bytecodeTransformerBuildItems.isEmpty()) {
            return new TransformedClassesBuildItem(Collections.emptyMap());
        }
        final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = new HashMap<>(
                bytecodeTransformerBuildItems.size());
        for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
            bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>())
                    .add(i.getVisitorFunction());
        }
        Map<String, Path> transformedToArchive = new ConcurrentHashMap<>();
        // now copy all the contents to the runner jar
        // we also record if any additional archives needed transformation
        // when we copy these archives we will remove the problematic classes
        final ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final ConcurrentLinkedDeque<Future<TransformedClassesBuildItem.TransformedClass>> transformed = new ConcurrentLinkedDeque<>();
        try {
            ClassLoader transformCl = Thread.currentThread().getContextClassLoader();
            for (Map.Entry<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> entry : bytecodeTransformers
                    .entrySet()) {
                String className = entry.getKey();
                ApplicationArchive archive = appArchives.containingArchive(entry.getKey());
                if (archive != null) {
                    List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = entry.getValue();
                    String classFileName = className.replace(".", "/") + ".class";
                    Path path = archive.getChildPath(classFileName);
                    transformedToArchive.put(classFileName, archive.getArchiveLocation());
                    transformed.add(executorPool.submit(new Callable<TransformedClassesBuildItem.TransformedClass>() {
                        @Override
                        public TransformedClassesBuildItem.TransformedClass call() throws Exception {
                            ClassLoader old = Thread.currentThread().getContextClassLoader();
                            try {
                                Thread.currentThread().setContextClassLoader(transformCl);
                                if (Files.size(path) > Integer.MAX_VALUE) {
                                    throw new RuntimeException(
                                            "Can't process class files larger than Integer.MAX_VALUE bytes");
                                }
                                ClassReader cr = new ClassReader(Files.readAllBytes(path));
                                ClassWriter writer = new QuarkusClassWriter(cr,
                                        ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                ClassVisitor visitor = writer;
                                for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                                    visitor = i.apply(className, visitor);
                                }
                                cr.accept(visitor, 0);
                                return new TransformedClassesBuildItem.TransformedClass(writer.toByteArray(), classFileName);
                            } finally {
                                Thread.currentThread().setContextClassLoader(old);
                            }
                        }
                    }));
                } else {
                    log.warnf("Cannot transform %s as it's containing application archive could not be found.",
                            entry.getKey());
                }
            }

        } finally {
            executorPool.shutdown();
        }
        Map<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJar = new HashMap<>();
        if (!transformed.isEmpty()) {
            for (Future<TransformedClassesBuildItem.TransformedClass> i : transformed) {
                final TransformedClassesBuildItem.TransformedClass res = i.get();
                transformedClassesByJar.computeIfAbsent(transformedToArchive.get(res.getFileName()), (a) -> new HashSet<>())
                        .add(res);
            }
        }
        return new TransformedClassesBuildItem(transformedClassesByJar);
    }

    @BuildStep
    TransformedArchiveBuildItem archives(ApplicationArchivesBuildItem archives,
            List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems) {
        if (bytecodeTransformerBuildItems.isEmpty()) {
            return new TransformedArchiveBuildItem(Collections.emptyList(), Collections.emptyMap(), Collections.emptySet());
        }
        Set<String> transformedClasses = new HashSet<>();
        for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
            transformedClasses.add(i.getClassToTransform());
        }
        List<ApplicationArchive> transformedArchive = new ArrayList<>();
        Map<String, Path> applicationClasses = new HashMap<>();
        Set<String> applicationJavaClassNames = new HashSet<>();
        try {
            for (ApplicationArchive aa : archives.getApplicationArchives()) {
                Path root = aa.getArchiveRoot();
                Map<String, Path> classes = new HashMap<>();
                Set<String> javaClassNames = new HashSet<>();
                AtomicBoolean transform = new AtomicBoolean();
                try (Stream<Path> stream = Files.walk(root)) {
                    stream.forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            if (path.toString().endsWith(".class")) {
                                String key = root.relativize(path).toString().replace('\\', '/');
                                classes.put(key, path);
                                String javaClassName = key.substring(0, key.length() - ".class".length()).replace("/", ".");
                                javaClassNames.add(javaClassName);
                                if (transformedClasses
                                        .contains(javaClassName)) {
                                    transform.set(true);
                                }
                            }
                        }
                    });
                }
                if (transform.get()) {
                    transformedArchive.add(aa);
                    applicationClasses.putAll(classes);
                    applicationJavaClassNames.addAll(javaClassNames);
                }
            }
            return new TransformedArchiveBuildItem(transformedArchive, applicationClasses, applicationJavaClassNames);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
