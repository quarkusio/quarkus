package io.quarkus.deployment.steps;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.QuarkusClassVisitor;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.index.ConstPoolScanner;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
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

    private static void reset() {
        lastTransformers = null;
        transformedClassesCache.clear();
    }

    @BuildStep
    TransformedClassesBuildItem handleClassTransformation(List<BytecodeTransformerBuildItem> bytecodeTransformerBuildItems,
            ApplicationArchivesBuildItem appArchives, LiveReloadBuildItem liveReloadBuildItem,
            LaunchModeBuildItem launchModeBuildItem, ClassLoadingConfig classLoadingConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem, List<RemovedResourceBuildItem> removedResourceBuildItems,
            ArchiveRootBuildItem archiveRoot, LaunchModeBuildItem launchMode, PackageConfig packageConfig,
            ExecutorService buildExecutor,
            CuratedApplicationShutdownBuildItem shutdown)
            throws ExecutionException, InterruptedException {
        if (bytecodeTransformerBuildItems.isEmpty() && classLoadingConfig.removedResources.isEmpty()
                && removedResourceBuildItems.isEmpty()) {
            return new TransformedClassesBuildItem(Collections.emptyMap());
        }
        final Map<String, List<BytecodeTransformerBuildItem>> bytecodeTransformers = new HashMap<>(
                bytecodeTransformerBuildItems.size());
        Set<String> noConstScanning = new HashSet<>();
        Map<String, Set<String>> constScanning = new HashMap<>();
        Set<String> nonCacheable = new HashSet<>();
        Map<String, Integer> classReaderOptions = new HashMap<>();
        for (BytecodeTransformerBuildItem i : bytecodeTransformerBuildItems) {
            bytecodeTransformers.computeIfAbsent(i.getClassToTransform(), (h) -> new ArrayList<>())
                    .add(i);
            if (i.getRequireConstPoolEntry() == null || i.getRequireConstPoolEntry().isEmpty()) {
                noConstScanning.add(i.getClassToTransform());
            } else {
                constScanning.computeIfAbsent(i.getClassToTransform(), (s) -> new HashSet<>())
                        .addAll(i.getRequireConstPoolEntry());
            }
            if (!i.isCacheable()) {
                nonCacheable.add(i.getClassToTransform());
            }
            classReaderOptions.merge(i.getClassToTransform(), i.getClassReaderOptions(),
                    // class reader options are bit flags (see org.objectweb.asm.ClassReader)
                    (oldValue, newValue) -> oldValue | newValue);
        }
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        Map<String, Path> transformedToArchive = new ConcurrentHashMap<>();
        // now copy all the contents to the runner jar
        // we also record if any additional archives needed transformation
        // when we copy these archives we will remove the problematic classes
        final ConcurrentLinkedDeque<Future<TransformedClassesBuildItem.TransformedClass>> transformed = new ConcurrentLinkedDeque<>();
        final Map<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJar = new HashMap<>();
        ClassLoader transformCl = Thread.currentThread().getContextClassLoader();
        shutdown.addCloseTask(ClassTransformingBuildStep::reset, true);
        lastTransformers = new BiFunction<String, byte[], byte[]>() {
            @Override
            public byte[] apply(String className, byte[] originalBytes) {

                List<BytecodeTransformerBuildItem> classTransformers = bytecodeTransformers.get(className);
                if (classTransformers == null) {
                    return originalBytes;
                }
                boolean continueOnFailure = classTransformers.stream()
                        .filter(a -> !a.isContinueOnFailure())
                        .findAny().isEmpty();
                List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = classTransformers.stream()
                        .sorted(Comparator.comparingInt(BytecodeTransformerBuildItem::getPriority))
                        .map(BytecodeTransformerBuildItem::getVisitorFunction)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                List<BiFunction<String, byte[], byte[]>> preVisitFunctions = classTransformers.stream()
                        .sorted(Comparator.comparingInt(BytecodeTransformerBuildItem::getPriority))
                        .map(BytecodeTransformerBuildItem::getInputTransformer)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                ClassLoader old = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(transformCl);
                    String classFileName = fromClassNameToResourceName(className);
                    List<ClassPathElement> archives = cl.getElementsWithResource(classFileName);
                    if (!archives.isEmpty()) {
                        ClassPathElement classPathElement = archives.get(0);
                        byte[] classData = classPathElement.getResource(classFileName).getData();
                        Set<String> constValues = constScanning.get(className);
                        if (constValues != null && !noConstScanning.contains(className)) {
                            if (!ConstPoolScanner.constPoolEntryPresent(classData, constValues)) {
                                return originalBytes;
                            }
                        }
                        byte[] data = transformClass(className, visitors, classData, preVisitFunctions,
                                classReaderOptions.getOrDefault(className, 0));
                        TransformedClassesBuildItem.TransformedClass transformedClass = new TransformedClassesBuildItem.TransformedClass(
                                className, data,
                                classFileName);
                        return transformedClass.getData();
                    } else {
                        return originalBytes;
                    }
                } catch (Throwable e) {
                    if (continueOnFailure) {
                        if (log.isDebugEnabled()) {
                            log.errorf(e, "Failed to transform %s", className);
                        } else {
                            log.errorf("Failed to transform %s", className);
                        }
                        return originalBytes;
                    } else {
                        throw e;
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(old);
                }
            }
        };
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
            String classFileName = fromClassNameToResourceName(className);
            List<ClassPathElement> archives = cl.getElementsWithResource(classFileName);
            if (!archives.isEmpty()) {
                ClassPathElement classPathElement = archives.get(0);
                Path jar = classPathElement.getRoot();
                if (jar == null) {
                    log.warnf("Cannot transform %s as its containing application archive could not be found.",
                            entry.getKey());
                    continue;
                }

                boolean continueOnFailure = entry.getValue().stream()
                        .filter(a -> !a.isContinueOnFailure())
                        .findAny().isEmpty();
                List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors = entry.getValue().stream()
                        .sorted(Comparator.comparingInt(BytecodeTransformerBuildItem::getPriority))
                        .map(BytecodeTransformerBuildItem::getVisitorFunction)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                List<BiFunction<String, byte[], byte[]>> preVisitFunctions = entry.getValue().stream()
                        .sorted(Comparator.comparingInt(BytecodeTransformerBuildItem::getPriority))
                        .map(BytecodeTransformerBuildItem::getInputTransformer)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                transformedToArchive.put(classFileName, jar);
                transformed.add(buildExecutor.submit(new Callable<TransformedClassesBuildItem.TransformedClass>() {
                    @Override
                    public TransformedClassesBuildItem.TransformedClass call() throws Exception {
                        ClassLoader old = Thread.currentThread().getContextClassLoader();
                        try {
                            byte[] classData = classPathElement.getResource(classFileName).getData();
                            Thread.currentThread().setContextClassLoader(transformCl);
                            Set<String> constValues = constScanning.get(className);
                            if (constValues != null && !noConstScanning.contains(className)) {
                                if (!ConstPoolScanner.constPoolEntryPresent(classData, constValues)) {
                                    return null;
                                }
                            }
                            byte[] data = transformClass(className, visitors, classData, preVisitFunctions,
                                    classReaderOptions.getOrDefault(className, 0));
                            TransformedClassesBuildItem.TransformedClass transformedClass = new TransformedClassesBuildItem.TransformedClass(
                                    className, data,
                                    classFileName);
                            if (cacheable && launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT
                                    && classData != null) {
                                transformedClassesCache.put(className, transformedClass);
                            }
                            return transformedClass;
                        } catch (Throwable e) {
                            if (continueOnFailure) {
                                if (log.isDebugEnabled()) {
                                    log.errorf(e, "Failed to transform %s", className);
                                } else {
                                    log.errorf("Failed to transform %s", className);
                                }
                                return null;
                            } else {
                                throw e;
                            }
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

        handleRemovedResources(classLoadingConfig, curateOutcomeBuildItem, transformedClassesByJar, removedResourceBuildItems);
        if (!transformed.isEmpty()) {
            for (Future<TransformedClassesBuildItem.TransformedClass> i : transformed) {
                final TransformedClassesBuildItem.TransformedClass res = i.get();
                if (res != null) {
                    handleTransformedClass(transformedToArchive, transformedClassesByJar, res);
                }
            }
        }

        if (packageConfig.writeTransformedBytecodeToBuildOutput() && (launchMode.getLaunchMode() == LaunchMode.NORMAL)) {
            // the idea here is to write the transformed classes into the build tool's output directory to make core coverage work

            for (Path path : archiveRoot.getRootDirectories()) {
                copyTransformedClasses(path, transformedClassesByJar.get(path));
            }
        }

        return new TransformedClassesBuildItem(transformedClassesByJar);
    }

    private void copyTransformedClasses(Path originalClassesPath,
            Set<TransformedClassesBuildItem.TransformedClass> transformedClasses) {
        if ((transformedClasses == null) || transformedClasses.isEmpty()) {
            return;
        }

        for (TransformedClassesBuildItem.TransformedClass transformedClass : transformedClasses) {
            String classFileName = transformedClass.getFileName();
            String[] fileNameParts = classFileName.split("/");
            Path classFilePath = originalClassesPath;
            for (String fileNamePart : fileNameParts) {
                classFilePath = classFilePath.resolve(fileNamePart);
            }
            try {
                Files.write(classFilePath, transformedClass.getData(), StandardOpenOption.WRITE);
            } catch (IOException e) {
                log.debug("Unable to overwrite file '" + classFilePath.toAbsolutePath() + "' with transformed class data");
            }
        }
    }

    private void handleRemovedResources(ClassLoadingConfig classLoadingConfig, CurateOutcomeBuildItem curateOutcomeBuildItem,
            Map<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJar,
            List<RemovedResourceBuildItem> removedResourceBuildItems) {
        //a little bit of a hack, but we use an empty transformed class to represent removed resources, as transforming a class removes it from the original archive
        Map<ArtifactKey, Set<String>> removed = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : classLoadingConfig.removedResources.entrySet()) {
            removed.put(new GACT(entry.getKey().split(":")), entry.getValue());
        }
        for (RemovedResourceBuildItem i : removedResourceBuildItems) {
            removed.computeIfAbsent(i.getArtifact(), k -> new HashSet<>()).addAll(i.getResources());
        }
        if (!removed.isEmpty()) {
            ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
            Collection<ResolvedDependency> runtimeDependencies = applicationModel.getRuntimeDependencies();
            List<ResolvedDependency> allArtifacts = new ArrayList<>(runtimeDependencies.size() + 1);
            allArtifacts.addAll(runtimeDependencies);
            allArtifacts.add(applicationModel.getAppArtifact());
            for (ResolvedDependency i : allArtifacts) {
                Set<String> filtered = removed.remove(i.getKey());
                if (filtered != null) {
                    for (Path path : i.getResolvedPaths()) {
                        transformedClassesByJar.computeIfAbsent(path, s -> new HashSet<>())
                                .addAll(filtered.stream()
                                        .map(file -> new TransformedClassesBuildItem.TransformedClass(null, null, file, false))
                                        .collect(Collectors.toSet()));
                    }
                }
            }
        }
        if (!removed.isEmpty()) {
            log.warn("Could not remove configured resources from the following artifacts as they were not found in the model: "
                    + removed);
        }
    }

    private byte[] transformClass(String className, List<BiFunction<String, ClassVisitor, ClassVisitor>> visitors,
            byte[] classData, List<BiFunction<String, byte[], byte[]>> preVisitFunctions, int classReaderOptions) {
        for (BiFunction<String, byte[], byte[]> i : preVisitFunctions) {
            classData = i.apply(className, classData);
            if (classData == null) {
                return null;
            }
        }
        byte[] data;
        if (!visitors.isEmpty()) {
            ClassReader cr = new ClassReader(classData);
            ClassWriter writer = new QuarkusClassWriter(cr,
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = writer;
            for (BiFunction<String, ClassVisitor, ClassVisitor> i : visitors) {
                visitor = i.apply(className, visitor);
                if (visitor instanceof QuarkusClassVisitor) {
                    ((QuarkusClassVisitor) visitor).setOriginalClassReaderOptions(classReaderOptions);
                }
            }
            cr.accept(visitor, classReaderOptions);
            data = writer.toByteArray();
        } else {
            data = classData;
        }
        var debugTransformedClassesDir = BootstrapDebug.transformedClassesDir();
        if (debugTransformedClassesDir != null) {
            File debugPath = new File(debugTransformedClassesDir);
            if (!debugPath.exists()) {
                debugPath.mkdir();
            }
            File classFile = new File(debugPath, fromClassNameToResourceName(className));
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
