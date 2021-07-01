package io.quarkus.deployment.steps;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.index.ConstPoolScanner;
import io.quarkus.runtime.LaunchMode;

public class ClassTransformingBuildStep {

    private static final Logger log = Logger.getLogger(ClassTransformingBuildStep.class);

    /**
     * Cache used for dev mode to save the result for classes that have not changed.
     */
    private static final Map<String, TransformedClassesBuildItem.TransformedClass> transformedClassesCache = new ConcurrentHashMap<>();
    private static volatile BiFunction<String, byte[], byte[]> lastTransformers;

    public static byte[] transform(String className, byte[] classData) {
        if (lastTransformers == null) {
            return classData;
        }

        return lastTransformers.apply(className, classData);
    }

    @BuildStep
    TransformedClassesBuildItem handleClassTransformation(List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems,
            ApplicationArchivesBuildItem appArchives, LiveReloadBuildItem liveReloadBuildItem,
            LaunchModeBuildItem launchModeBuildItem)
            throws ExecutionException, InterruptedException {
        if (bytecodeTransformerBuildItems.isEmpty()) {
            return new TransformedClassesBuildItem(Collections.emptyMap());
        }
        final Map<String, List<BytecodeTransformerBuildItem>> bytecodeTransformers = new HashMap<>(
                bytecodeTransformerBuildItems.size());
        Set<String> noConstScanning = new HashSet<>();
        Map<String, Set<String>> constScanning = new HashMap<>();
        Set<String> eager = new HashSet<>();
        Set<String> nonCacheable = new HashSet<>();
        for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
            bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>())
                    .add(i);
            if (i.getRequireConstPoolEntry() == null || i.getRequireConstPoolEntry().isEmpty()) {
                noConstScanning.add(i.getClassToTransform());
            } else {
                constScanning.computeIfAbsent(i.getClassToTransform(), (s) -> new HashSet<>())
                        .addAll(i.getRequireConstPoolEntry());
            }
            if (i.isEager()) {
                eager.add(i.getClassToTransform());
            }
            if (!i.isCacheable()) {
                nonCacheable.add(i.getClassToTransform());
            }
        }
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        Map<String, Path> transformedToArchive = new ConcurrentHashMap<>();
        // now copy all the contents to the runner jar
        // we also record if any additional archives needed transformation
        // when we copy these archives we will remove the problematic classes
        final ExecutorService executorPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final ConcurrentLinkedDeque<Future<TransformedClassesBuildItem.TransformedClass>> transformed = new ConcurrentLinkedDeque<>();
        final Map<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJar = new HashMap<>();
        ClassLoader transformCl = Thread.currentThread().getContextClassLoader();
        lastTransformers = new BiFunction<String, byte[], byte[]>() {
            @Override
            public byte[] apply(String className, byte[] originalBytes) {

                List<BytecodeTransformerBuildItem> classTransformers = bytecodeTransformers.get(className);
                if (classTransformers == null) {
                    return originalBytes;
                }
                List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = classTransformers.stream()
                        .map(BytecodeTransformerBuildItem::getVisitorFunction).filter(Objects::nonNull)
                        .collect(Collectors.toList());
                List<BiFunction<String, byte[], byte[]>> preVisitFunctions = classTransformers.stream()
                        .map(BytecodeTransformerBuildItem::getInputTransformer).filter(Objects::nonNull)
                        .collect(Collectors.toList());
                ClassLoader old = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(transformCl);
                    String classFileName = className.replace('.', '/') + ".class";
                    List<ClassPathElement> archives = cl.getElementsWithResource(classFileName);
                    if (!archives.isEmpty()) {
                        ClassPathElement classPathElement = archives.get(0);
                        Path jar = classPathElement.getRoot();
                        byte[] classData = classPathElement.getResource(classFileName).getData();
                        Set<String> constValues = constScanning.get(className);
                        if (constValues != null && !noConstScanning.contains(className)) {
                            if (!ConstPoolScanner.constPoolEntryPresent(classData, constValues)) {
                                return originalBytes;
                            }
                        }
                        byte[] data = transformClass(className, visitors, classData, preVisitFunctions);
                        TransformedClassesBuildItem.TransformedClass transformedClass = new TransformedClassesBuildItem.TransformedClass(
                                className, data,
                                classFileName, eager.contains(className));
                        return transformedClass.getData();
                    } else {
                        return originalBytes;
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(old);
                }
            }
        };
        try {
            for (Map.Entry<String, List<BytecodeTransformerBuildItem>> entry : bytecodeTransformers
                    .entrySet()) {
                String className = entry.getKey();
                boolean cacheable = !nonCacheable.contains(className);
                if (cacheable && transformedClassesCache.containsKey(className)) {
                    if (liveReloadBuildItem.getChangeInformation() != null) {
                        if (!liveReloadBuildItem.getChangeInformation().getChangedClasses().contains(className)) {
                            //we can use the cached transformation
                            handleTransformedClass(transformedToArchive, transformedClassesByJar,
                                    transformedClassesCache.get(className));
                            continue;
                        }
                    }
                }
                String classFileName = className.replace('.', '/') + ".class";
                List<ClassPathElement> archives = cl.getElementsWithResource(classFileName);
                if (!archives.isEmpty()) {
                    ClassPathElement classPathElement = archives.get(0);
                    Path jar = classPathElement.getRoot();
                    if (jar == null) {
                        log.warnf("Cannot transform %s as its containing application archive could not be found.",
                                entry.getKey());
                        continue;
                    }
                    List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = entry.getValue().stream()
                            .map(BytecodeTransformerBuildItem::getVisitorFunction).filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    List<BiFunction<String, byte[], byte[]>> preVisitFunctions = entry.getValue().stream()
                            .map(BytecodeTransformerBuildItem::getInputTransformer).filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    transformedToArchive.put(classFileName, jar);
                    transformed.add(executorPool.submit(new Callable<TransformedClassesBuildItem.TransformedClass>() {
                        @Override
                        public TransformedClassesBuildItem.TransformedClass call() throws Exception {
                            ClassLoader old = Thread.currentThread().getContextClassLoader();
                            try {
                                Thread.currentThread().setContextClassLoader(transformCl);
                                Set<String> constValues = constScanning.get(className);
                                byte[] classData = classPathElement.getResource(classFileName).getData();
                                if (constValues != null && !noConstScanning.contains(className)) {
                                    if (!ConstPoolScanner.constPoolEntryPresent(classData, constValues)) {
                                        return null;
                                    }
                                }
                                byte[] data = transformClass(className, visitors, classData, preVisitFunctions);
                                TransformedClassesBuildItem.TransformedClass transformedClass = new TransformedClassesBuildItem.TransformedClass(
                                        className, data,
                                        classFileName, eager.contains(className));
                                if (cacheable && launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT) {
                                    transformedClassesCache.put(className, transformedClass);
                                }
                                return transformedClass;
                            } finally {
                                Thread.currentThread().setContextClassLoader(old);
                            }
                        }
                    }));
                } else {
                    log.warnf("Cannot transform %s as its containing application archive could not be found.",
                            entry.getKey());
                }
            }

        } finally {
            executorPool.shutdown();
        }
        if (!transformed.isEmpty()) {
            for (Future<TransformedClassesBuildItem.TransformedClass> i : transformed) {
                final TransformedClassesBuildItem.TransformedClass res = i.get();
                if (res != null) {
                    handleTransformedClass(transformedToArchive, transformedClassesByJar, res);
                }
            }
        }
        return new TransformedClassesBuildItem(transformedClassesByJar);
    }

    private byte[] transformClass(String className, List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors,
            byte[] classData, List<BiFunction<String, byte[], byte[]>> preVisitFunctions) {
        for (BiFunction<String, byte[], byte[]> i : preVisitFunctions) {
            classData = i.apply(className, classData);
        }
        byte[] data;
        if (!visitors.isEmpty()) {
            ClassReader cr = new ClassReader(classData);
            ClassWriter writer = new QuarkusClassWriter(cr,
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = writer;
            for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                visitor = i.apply(className, visitor);
            }
            cr.accept(visitor, 0);
            data = writer.toByteArray();
        } else {
            data = classData;
        }
        if (BootstrapDebug.DEBUG_TRANSFORMED_CLASSES_DIR != null) {
            File debugPath = new File(BootstrapDebug.DEBUG_TRANSFORMED_CLASSES_DIR);
            if (!debugPath.exists()) {
                debugPath.mkdir();
            }
            File classFile = new File(debugPath, className.replace('.', '/') + ".class");
            classFile.getParentFile().mkdirs();
            try (FileOutputStream classWriter = new FileOutputStream(classFile)) {
                classWriter.write(data);
            } catch (Exception e) {
                log.errorf(e, "Failed to write transformed class %s", className);
            }
            log.infof("Wrote transformed class to %s", classFile.getAbsolutePath());
        }
        return data;
    }

    private void handleTransformedClass(Map<String, Path> transformedToArchive,
            Map<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJar,
            TransformedClassesBuildItem.TransformedClass res) {
        transformedClassesByJar.computeIfAbsent(transformedToArchive.get(res.getFileName()), (a) -> new HashSet<>())
                .add(res);
    }

}
