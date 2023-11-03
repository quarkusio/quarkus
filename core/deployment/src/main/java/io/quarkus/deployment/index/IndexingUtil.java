package io.quarkus.deployment.index;

import static io.quarkus.bootstrap.classloading.JarClassPathElement.JAVA_VERSION;
import static io.quarkus.bootstrap.classloading.JarClassPathElement.META_INF_VERSIONS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassSummary;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.UnsupportedVersion;
import org.jboss.logging.Logger;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;
import io.smallrye.common.io.jar.JarFiles;

public class IndexingUtil {

    private static final Logger log = Logger.getLogger("io.quarkus.deployment.index");

    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    public static final String JANDEX_INDEX = "META-INF/jandex.idx";

    // At least Jandex 2.1 is needed
    private static final int REQUIRED_INDEX_VERSION = 8;

    public static Index indexJar(Path path) throws IOException {
        return indexJar(path.toFile(), Collections.emptySet());
    }

    public static Index indexJar(File file) throws IOException {
        return indexJar(file, Collections.emptySet());
    }

    public static Index indexJar(Path path, Set<String> removed) throws IOException {
        return indexJar(path.toFile(), removed);
    }

    public static Index indexTree(OpenPathTree tree, Set<String> removed) throws IOException {
        if (removed == null) {
            final Index i = tree.apply(JANDEX_INDEX, MetaInfJandexReader.getInstance());
            if (i != null) {
                return i;
            }
        }
        final Indexer indexer = new Indexer();
        final PathTreeIndexer treeIndexer = new PathTreeIndexer(indexer, removed);
        tree.walk(treeIndexer);
        return indexer.complete();
    }

    public static Index indexJar(File file, Set<String> removed) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            ZipEntry existing = jarFile.getEntry(JANDEX_INDEX);
            if (existing != null && removed == null) {
                try (InputStream in = jarFile.getInputStream(existing)) {
                    IndexReader reader = new IndexReader(in);
                    if (reader.getIndexVersion() < REQUIRED_INDEX_VERSION) {
                        log.warnf(
                                "Re-indexing %s - at least Jandex 2.1 must be used to index an application dependency",
                                file);
                        return indexJar(jarFile, removed);
                    } else {
                        try {
                            return reader.read();
                        } catch (UnsupportedVersion e) {
                            throw new UnsupportedVersion("Can't read Jandex index from " + file + ": " + e.getMessage());
                        }
                    }
                }
            }
            return indexJar(jarFile, removed);
        }
    }

    private static Index indexJar(JarFile file, Set<String> removed) throws IOException {
        Indexer indexer = new Indexer();
        Enumeration<JarEntry> e = file.entries();
        boolean multiRelease = JarFiles.isMultiRelease(file);
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            if (removed != null && removed.contains(entry.getName())) {
                continue;
            }
            if (entry.getName().endsWith(".class")) {
                if (multiRelease && entry.getName().startsWith(META_INF_VERSIONS)) {
                    String part = entry.getName().substring(META_INF_VERSIONS.length());
                    int slash = part.indexOf("/");
                    if (slash != -1) {
                        try {
                            int ver = Integer.parseInt(part.substring(0, slash));
                            if (ver <= JAVA_VERSION) {
                                try (InputStream inputStream = file.getInputStream(entry)) {
                                    indexer.index(inputStream);
                                }
                            }
                        } catch (NumberFormatException ex) {
                            log.debug("Failed to parse META-INF/versions entry", ex);
                        }
                    }
                } else {
                    try (InputStream inputStream = file.getInputStream(entry)) {
                        indexer.index(inputStream);
                    }
                }
            }
        }
        return indexer.complete();
    }

    public static void indexClass(String className, Indexer indexer, IndexView quarkusIndex,
            Set<DotName> additionalIndex, ClassLoader classLoader) {
        indexClass(className, indexer, quarkusIndex, additionalIndex, new HashSet<>(), classLoader);
    }

    public static void indexClass(String className, Indexer indexer, IndexView quarkusIndex,
            Set<DotName> additionalIndex, Set<DotName> knownMissingClasses, ClassLoader classLoader) {
        DotName classDotName = DotName.createSimple(className);
        if (additionalIndex.contains(classDotName)) {
            return;
        }

        DotName superclassName;
        Set<DotName> annotationNames;

        ClassInfo classInfo = quarkusIndex.getClassByName(classDotName);
        if (classInfo == null) {
            log.debugf("Index class: %s", className);
            try (InputStream stream = IoUtil.readClass(classLoader, className)) {
                ClassSummary summary = indexer.indexWithSummary(stream);
                additionalIndex.add(summary.name());
                superclassName = summary.superclassName();
                annotationNames = summary.annotations();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to index: " + className, e);
            }
        } else {
            // The class could be indexed by quarkus - we still need to distinguish framework classes
            additionalIndex.add(classDotName);
            superclassName = classInfo.superName();
            annotationNames = classInfo.annotationsMap().keySet();
        }

        for (DotName annotationName : annotationNames) {
            if (!additionalIndex.contains(annotationName) && quarkusIndex.getClassByName(annotationName) == null) {
                try (InputStream annotationStream = IoUtil.readClass(classLoader, annotationName.toString())) {
                    if (annotationStream == null) {
                        log.debugf("Could not index annotation: %s (missing class or dependency)", annotationName);
                        knownMissingClasses.add(annotationName);
                    } else {
                        log.debugf("Index annotation: %s", annotationName);
                        indexClass(annotationName.toString(), indexer, quarkusIndex, additionalIndex, knownMissingClasses,
                                classLoader);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + className, e);
                }
            }
        }
        if (superclassName != null && !superclassName.equals(OBJECT)) {
            indexClass(superclassName.toString(), indexer, quarkusIndex, additionalIndex, knownMissingClasses, classLoader);
        }
    }

    public static void indexClass(String className, Indexer indexer,
            IndexView quarkusIndex, Set<DotName> additionalIndex, ClassLoader classLoader, byte[] beanData) {
        indexClass(className, indexer, quarkusIndex, additionalIndex, new HashSet<>(), classLoader, beanData);
    }

    public static void indexClass(String className, Indexer indexer,
            IndexView quarkusIndex, Set<DotName> additionalIndex, Set<DotName> knownMissingClasses,
            ClassLoader classLoader, byte[] beanData) {
        DotName classDotName = DotName.createSimple(className);
        if (additionalIndex.contains(classDotName)) {
            return;
        }

        Set<DotName> annotationNames;

        ClassInfo classInfo = quarkusIndex.getClassByName(classDotName);
        if (classInfo == null) {
            log.debugf("Index class: %s", className);
            try (InputStream stream = new ByteArrayInputStream(beanData)) {
                ClassSummary summary = indexer.indexWithSummary(stream);
                additionalIndex.add(summary.name());
                annotationNames = summary.annotations();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + className, e);
            }
        } else {
            // The class could be indexed by quarkus - we still need to distinguish framework classes
            additionalIndex.add(classDotName);
            annotationNames = classInfo.annotationsMap().keySet();
        }

        for (DotName annotationName : annotationNames) {
            if (!additionalIndex.contains(annotationName) && quarkusIndex.getClassByName(annotationName) == null) {
                try (InputStream annotationStream = IoUtil.readClass(classLoader, annotationName.toString())) {
                    if (annotationStream == null) {
                        log.debugf("Could not index annotation: %s (missing class or dependency)", annotationName);
                        knownMissingClasses.add(annotationName);
                    } else {
                        log.debugf("Index annotation: %s", annotationName);
                        indexClass(annotationName.toString(), indexer, quarkusIndex, additionalIndex, knownMissingClasses,
                                classLoader);
                        additionalIndex.add(annotationName);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + className, e);
                }
            }
        }
    }

    private static class PathTreeIndexer implements PathVisitor {

        final Indexer indexer;
        final Set<String> removed;

        PathTreeIndexer(Indexer indexer, Set<String> removed) {
            this.indexer = indexer;
            this.removed = removed;
        }

        @Override
        public void visitPath(PathVisit visit) {
            final Path fileName = visit.getPath().getFileName();
            if (fileName == null ||
                    !fileName.toString().endsWith(".class") ||
                    Files.isDirectory(visit.getPath()) ||
                    removed != null && removed.contains(visit.getRelativePath("/"))) {
                return;
            }
            try (InputStream inputStream = Files.newInputStream(visit.getPath())) {
                indexer.index(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static class MetaInfJandexReader implements Function<PathVisit, Index> {
        private static MetaInfJandexReader instance;

        private static MetaInfJandexReader getInstance() {
            return instance == null ? instance = new MetaInfJandexReader() : instance;
        }

        @Override
        public Index apply(PathVisit visit) {
            if (visit == null) {
                return null;
            }
            try (InputStream in = Files.newInputStream(visit.getPath())) {
                IndexReader reader = new IndexReader(in);
                try {
                    if (reader.getIndexVersion() < REQUIRED_INDEX_VERSION) {
                        log.warnf(
                                "Re-indexing %s - at least Jandex 2.1 must be used to index an application dependency",
                                visit.getPath());
                        return null;
                    }
                    return reader.read();
                } catch (UnsupportedVersion e) {
                    throw new UnsupportedVersion(
                            "Can't read Jandex index from " + visit.getPath() + ": " + e.getMessage());
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Can't read Jandex index from " + visit.getPath(), e);
            }
        }
    }
}
