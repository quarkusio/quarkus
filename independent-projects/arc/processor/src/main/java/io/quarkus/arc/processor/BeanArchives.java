package io.quarkus.arc.processor;

import io.quarkus.arc.ActivateRequestContextInterceptor;
import io.quarkus.arc.InjectableRequestContextController;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Intercepted;
import javax.inject.Named;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;

public final class BeanArchives {

    /**
     * 
     * @param applicationIndexes
     * @return the final bean archive index
     */
    public static IndexView buildBeanArchiveIndex(IndexView... applicationIndexes) {
        List<IndexView> indexes = new ArrayList<>();
        Collections.addAll(indexes, applicationIndexes);
        indexes.add(buildAdditionalIndex());
        return new IndexWrapper(CompositeIndex.create(indexes));
    }

    private static IndexView buildAdditionalIndex() {
        Indexer indexer = new Indexer();
        // CDI API
        index(indexer, ActivateRequestContext.class.getName());
        index(indexer, Default.class.getName());
        index(indexer, Any.class.getName());
        index(indexer, Named.class.getName());
        index(indexer, Initialized.class.getName());
        index(indexer, BeforeDestroyed.class.getName());
        index(indexer, Destroyed.class.getName());
        index(indexer, Intercepted.class.getName());
        // Arc built-in beans
        index(indexer, ActivateRequestContextInterceptor.class.getName());
        index(indexer, InjectableRequestContextController.class.getName());
        return indexer.complete();
    }

    /**
     * This wrapper is used to index JDK classes on demand.
     */
    static class IndexWrapper implements IndexView {

        private final Map<DotName, ClassInfo> additionalClasses;

        private final IndexView index;

        public IndexWrapper(IndexView index) {
            this.index = index;
            this.additionalClasses = new ConcurrentHashMap<>();
        }

        @Override
        public Collection<ClassInfo> getKnownClasses() {
            return index.getKnownClasses();
        }

        @Override
        public ClassInfo getClassByName(DotName className) {
            ClassInfo classInfo = index.getClassByName(className);
            if (classInfo == null) {
                return additionalClasses.computeIfAbsent(className, name -> {
                    BeanProcessor.LOGGER.debugf("Index: %s", className);
                    Indexer indexer = new Indexer();
                    BeanArchives.index(indexer, className.toString());
                    Index index = indexer.complete();
                    return index.getClassByName(name);
                });
            }
            return classInfo;
        }

        @Override
        public Collection<ClassInfo> getKnownDirectSubclasses(DotName className) {
            if (additionalClasses.isEmpty()) {
                return index.getKnownDirectSubclasses(className);
            }
            Set<ClassInfo> directSubclasses = new HashSet<ClassInfo>(index.getKnownDirectSubclasses(className));
            for (ClassInfo additional : additionalClasses.values()) {
                if (className.equals(additional.superName())) {
                    directSubclasses.add(additional);
                }
            }
            return directSubclasses;
        }

        @Override
        public Collection<ClassInfo> getAllKnownSubclasses(DotName className) {
            if (additionalClasses.isEmpty()) {
                return index.getAllKnownSubclasses(className);
            }
            final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
            final Set<DotName> processedClasses = new HashSet<DotName>();
            getAllKnownSubClasses(className, allKnown, processedClasses);
            return allKnown;
        }

        @Override
        public Collection<ClassInfo> getKnownDirectImplementors(DotName className) {
            if (additionalClasses.isEmpty()) {
                return index.getKnownDirectImplementors(className);
            }
            Set<ClassInfo> directImplementors = new HashSet<ClassInfo>(index.getKnownDirectImplementors(className));
            for (ClassInfo additional : additionalClasses.values()) {
                for (Type interfaceType : additional.interfaceTypes()) {
                    if (className.equals(interfaceType.name())) {
                        directImplementors.add(additional);
                        break;
                    }
                }
            }
            return directImplementors;
        }

        @Override
        public Collection<ClassInfo> getAllKnownImplementors(DotName interfaceName) {
            if (additionalClasses.isEmpty()) {
                return index.getAllKnownImplementors(interfaceName);
            }
            final Set<ClassInfo> allKnown = new HashSet<ClassInfo>();
            final Set<DotName> subInterfacesToProcess = new HashSet<DotName>();
            final Set<DotName> processedClasses = new HashSet<DotName>();
            subInterfacesToProcess.add(interfaceName);
            while (!subInterfacesToProcess.isEmpty()) {
                final Iterator<DotName> toProcess = subInterfacesToProcess.iterator();
                DotName name = toProcess.next();
                toProcess.remove();
                processedClasses.add(name);
                getKnownImplementors(name, allKnown, subInterfacesToProcess, processedClasses);
            }
            return allKnown;
        }

        @Override
        public Collection<AnnotationInstance> getAnnotations(DotName annotationName) {
            return index.getAnnotations(annotationName);
        }

        private void getAllKnownSubClasses(DotName className, Set<ClassInfo> allKnown, Set<DotName> processedClasses) {
            final Set<DotName> subClassesToProcess = new HashSet<DotName>();
            subClassesToProcess.add(className);
            while (!subClassesToProcess.isEmpty()) {
                final Iterator<DotName> toProcess = subClassesToProcess.iterator();
                DotName name = toProcess.next();
                toProcess.remove();
                processedClasses.add(name);
                getAllKnownSubClasses(name, allKnown, subClassesToProcess, processedClasses);
            }
        }

        private void getAllKnownSubClasses(DotName name, Set<ClassInfo> allKnown, Set<DotName> subClassesToProcess,
                Set<DotName> processedClasses) {
            final Collection<ClassInfo> directSubclasses = getKnownDirectSubclasses(name);
            if (directSubclasses != null) {
                for (final ClassInfo clazz : directSubclasses) {
                    final DotName className = clazz.name();
                    if (!processedClasses.contains(className)) {
                        allKnown.add(clazz);
                        subClassesToProcess.add(className);
                    }
                }
            }
        }

        private void getKnownImplementors(DotName name, Set<ClassInfo> allKnown, Set<DotName> subInterfacesToProcess,
                Set<DotName> processedClasses) {
            final Collection<ClassInfo> list = getKnownDirectImplementors(name);
            if (list != null) {
                for (final ClassInfo clazz : list) {
                    final DotName className = clazz.name();
                    if (!processedClasses.contains(className)) {
                        if (Modifier.isInterface(clazz.flags())) {
                            subInterfacesToProcess.add(className);
                        } else {
                            if (!allKnown.contains(clazz)) {
                                allKnown.add(clazz);
                                processedClasses.add(className);
                                getAllKnownSubClasses(className, allKnown, processedClasses);
                            }
                        }
                    }
                }
            }
        }

    }

    static void index(Indexer indexer, String className) {
        try (InputStream stream = BeanProcessor.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            indexer.index(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to index: " + className, e);
        }
    }

}
