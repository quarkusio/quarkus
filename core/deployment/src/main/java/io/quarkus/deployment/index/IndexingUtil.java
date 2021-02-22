package io.quarkus.deployment.index;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.deployment.util.IoUtil;

public class IndexingUtil {

    private static final Logger log = Logger.getLogger("io.quarkus.deployment.index");

    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    public static final String JANDEX_INDEX = "META-INF/jandex.idx";

    // At least Jandex 2.1 is needed
    private static final int REQUIRED_INDEX_VERSION = 8;

    public static Index indexJar(Path path) throws IOException {
        return indexJar(path.toFile());
    }

    public static Index indexJar(File file) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            ZipEntry existing = jarFile.getEntry(JANDEX_INDEX);
            if (existing != null) {
                try (InputStream in = jarFile.getInputStream(existing)) {
                    IndexReader reader = new IndexReader(in);
                    if (reader.getIndexVersion() < REQUIRED_INDEX_VERSION) {
                        log.warnf(
                                "Re-indexing %s - at least Jandex 2.1 must be used to index an application dependency",
                                file);
                        return indexJar(jarFile);
                    } else {
                        return reader.read();
                    }
                }
            }
            return indexJar(jarFile);
        }
    }

    private static Index indexJar(JarFile file) throws IOException {
        Indexer indexer = new Indexer();
        Enumeration<JarEntry> e = file.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            if (entry.getName().endsWith(".class")) {
                try (InputStream inputStream = file.getInputStream(entry)) {
                    indexer.index(inputStream);
                }
            }
        }
        return indexer.complete();
    }

    public static void indexClass(String beanClass, Indexer indexer, IndexView quarkusIndex,
            Set<DotName> additionalIndex, ClassLoader classLoader) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = quarkusIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            log.debugf("Index bean class: %s", beanClass);
            try (InputStream stream = IoUtil.readClass(classLoader, beanClass)) {
                beanInfo = indexer.index(stream);
                additionalIndex.add(beanInfo.name());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to index: " + beanClass, e);
            }
        } else {
            // The class could be indexed by quarkus - we still need to distinguish framework classes
            additionalIndex.add(beanClassName);
        }
        for (DotName annotationName : beanInfo.annotations().keySet()) {
            if (!additionalIndex.contains(annotationName) && quarkusIndex.getClassByName(annotationName) == null) {
                try (InputStream annotationStream = IoUtil.readClass(classLoader, annotationName.toString())) {
                    if (annotationStream == null) {
                        log.debugf("Could not index annotation: %s (missing class or dependency)", annotationName);
                    } else {
                        log.debugf("Index annotation: %s", annotationName);
                        indexer.index(annotationStream);
                        additionalIndex.add(annotationName);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + beanClass, e);
                }
            }
        }
        if (beanInfo.superName() != null && !beanInfo.superName().equals(OBJECT)) {
            indexClass(beanInfo.superName().toString(), indexer, quarkusIndex, additionalIndex, classLoader);
        }
    }

    public static void indexClass(String beanClass, Indexer indexer,
            IndexView quarkusIndex, Set<DotName> additionalIndex,
            ClassLoader classLoader, byte[] beanData) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = quarkusIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            log.debugf("Index bean class: %s", beanClass);
            try (InputStream stream = new ByteArrayInputStream(beanData)) {
                beanInfo = indexer.index(stream);
                additionalIndex.add(beanInfo.name());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + beanClass, e);
            }
        } else {
            // The class could be indexed by quarkus - we still need to distinguish framework classes
            additionalIndex.add(beanClassName);
        }
        for (DotName annotationName : beanInfo.annotations().keySet()) {
            if (!additionalIndex.contains(annotationName) && quarkusIndex.getClassByName(annotationName) == null) {
                try (InputStream annotationStream = IoUtil.readClass(classLoader, annotationName.toString())) {
                    log.debugf("Index annotation: %s", annotationName);
                    indexer.index(annotationStream);
                    additionalIndex.add(annotationName);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + beanClass, e);
                }
            }
        }
    }

}
