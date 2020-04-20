package io.quarkus.deployment.steps;

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
import java.util.function.BiFunction;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;

public class ClassTransformingBuildStep {

    private static final Logger log = Logger.getLogger(ClassTransformingBuildStep.class);

    @BuildStep
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
                    archive.processEntry(classFileName, (classFile, jar) -> {
                        transformedToArchive.put(classFileName, jar);
                        transformed.add(executorPool.submit(new Callable<TransformedClassesBuildItem.TransformedClass>() {
                            @Override
                            public TransformedClassesBuildItem.TransformedClass call() throws Exception {
                                ClassLoader old = Thread.currentThread().getContextClassLoader();
                                try {
                                    Thread.currentThread().setContextClassLoader(transformCl);
                                    if (Files.size(classFile) > Integer.MAX_VALUE) {
                                        throw new RuntimeException(
                                                "Can't process class files larger than Integer.MAX_VALUE bytes");
                                    }
                                    ClassReader cr = new ClassReader(Files.readAllBytes(classFile));
                                    ClassWriter writer = new QuarkusClassWriter(cr,
                                            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                    ClassVisitor visitor = writer;
                                    for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                                        visitor = i.apply(className, visitor);
                                    }
                                    cr.accept(visitor, 0);
                                    return new TransformedClassesBuildItem.TransformedClass(writer.toByteArray(),
                                            classFileName);
                                } finally {
                                    Thread.currentThread().setContextClassLoader(old);
                                }
                            }
                        }));
                    });
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

}
