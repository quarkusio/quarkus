package io.quarkus.deployment.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import io.quarkus.deployment.util.IoUtil;

public class IndexingUtil {

    private static final Logger log = Logger.getLogger("io.quarkus.deployment.index");

    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());

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
