package io.quarkus.deployment.steps;

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

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.index.ConstPoolScanner;

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
        Set<String> noConstScanning = new HashSet<>();
        Map<String, Set<String>> constScanning = new HashMap<>();
        for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
            bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>())
                    .add(i.getVisitorFunction());
            if (i.getRequireConstPoolEntry() == null || i.getRequireConstPoolEntry().isEmpty()) {
                noConstScanning.add(i.getClassToTransform());
            } else {
                constScanning.computeIfAbsent(i.getClassToTransform(), (s) -> new HashSet<>())
                        .addAll(i.getRequireConstPoolEntry());
            }
        }
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
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
                String classFileName = className.replace(".", "/") + ".class";
                List<ClassPathElement> archives = cl.getElementsWithResource(classFileName);
                if (!archives.isEmpty()) {
                    ClassPathElement classPathElement = archives.get(0);
                    Path jar = classPathElement.getRoot();
                    if (jar == null) {
                        log.warnf("Cannot transform %s as it's containing application archive could not be found.",
                                entry.getKey());
                        continue;
                    }
                    List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = entry.getValue();
                    transformedToArchive.put(classFileName, jar);
                    transformed.add(executorPool.submit(new Callable<TransformedClassesBuildItem.TransformedClass>() {
                        @Override
                        public TransformedClassesBuildItem.TransformedClass call() throws Exception {
                            ClassLoader old = Thread.currentThread().getContextClassLoader();
                            try {
                                Thread.currentThread().setContextClassLoader(transformCl);
                                byte[] classData = classPathElement.getResource(classFileName).getData();
                                Set<String> constValues = constScanning.get(className);
                                if (constValues != null && !noConstScanning.contains(className)) {
                                    if (!ConstPoolScanner.constPoolEntryPresent(classData, constValues)) {
                                        return null;
                                    }
                                }
                                ClassReader cr = new ClassReader(classData);
                                ClassWriter writer = new QuarkusClassWriter(cr,
                                        ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                                ClassVisitor visitor = writer;
                                for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                                    visitor = i.apply(className, visitor);
                                }
                                cr.accept(visitor, 0);
                                return new TransformedClassesBuildItem.TransformedClass(writer.toByteArray(),
                                        classFileName, className);
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
                if (res != null) {
                    transformedClassesByJar.computeIfAbsent(transformedToArchive.get(res.getFileName()), (a) -> new HashSet<>())
                            .add(res);
                }
            }
        }
        return new TransformedClassesBuildItem(transformedClassesByJar);
    }

}
