package org.jboss.shamrock.deployment.index;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.jboss.shamrock.deployment.util.IoUtil;

public class IndexingUtil {

    private static final Logger log = Logger.getLogger("org.jboss.shamrock.deployment.index");

    public static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    public static void indexClass(String beanClass, Indexer indexer, IndexView shamrockIndex,
                                  Set<DotName> additionalIndex, ClassLoader classLoader) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = shamrockIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            log.debugf("Index bean class: %s", beanClass);
            try (InputStream stream = IoUtil.readClass(classLoader, beanClass)) {
                beanInfo = indexer.index(stream);
                additionalIndex.add(beanInfo.name());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + beanClass, e);
            }
        } else {
            // The class could be indexed by shamrock - we still need to distinguish framework classes
            additionalIndex.add(beanClassName);
        }
        for (DotName annotationName : beanInfo.annotations().keySet()) {
            if (!additionalIndex.contains(annotationName) && shamrockIndex.getClassByName(annotationName) == null) {
                try (InputStream annotationStream = IoUtil.readClass(classLoader, annotationName.toString())) {
                    log.debugf("Index annotation: %s", annotationName);
                    indexer.index(annotationStream);
                    additionalIndex.add(annotationName);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to index: " + beanClass, e);
                }
            }
        }
        if (!beanInfo.superName().equals(OBJECT)) {
            indexClass(beanInfo.superName().toString(), indexer, shamrockIndex, additionalIndex, classLoader);
        }
    }

    public static void indexClass(String beanClass, Indexer indexer,
                                  IndexView shamrockIndex, Set<DotName> additionalIndex,
                                  ClassLoader classLoader, byte[] beanData) {
        DotName beanClassName = DotName.createSimple(beanClass);
        if (additionalIndex.contains(beanClassName)) {
            return;
        }
        ClassInfo beanInfo = shamrockIndex.getClassByName(beanClassName);
        if (beanInfo == null) {
            log.debugf("Index bean class: %s", beanClass);
            try (InputStream stream = new ByteArrayInputStream(beanData)) {
                beanInfo = indexer.index(stream);
                additionalIndex.add(beanInfo.name());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + beanClass, e);
            }
        } else {
            // The class could be indexed by shamrock - we still need to distinguish framework classes
            additionalIndex.add(beanClassName);
        }
        for (DotName annotationName : beanInfo.annotations().keySet()) {
            if (!additionalIndex.contains(annotationName) && shamrockIndex.getClassByName(annotationName) == null) {
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
