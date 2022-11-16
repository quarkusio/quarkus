package io.quarkus.arc.processor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.Model;
import javax.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.ModuleInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.All;
import io.quarkus.arc.Lock;
import io.quarkus.arc.impl.ActivateRequestContextInterceptor;
import io.quarkus.arc.impl.DefaultAsyncObserverExceptionHandler;
import io.quarkus.arc.impl.Identified;
import io.quarkus.arc.impl.InjectableRequestContextController;
import io.quarkus.arc.impl.LockInterceptor;

public final class BeanArchives {

    private static final Logger LOGGER = Logger.getLogger(BeanArchives.class);

    /**
     *
     * @param applicationIndexes
     * @return the final bean archive index
     */
    public static IndexView buildBeanArchiveIndex(ClassLoader deploymentClassLoader,
            Map<DotName, Optional<ClassInfo>> additionalClasses, IndexView... applicationIndexes) {
        List<IndexView> indexes = new ArrayList<>();
        Collections.addAll(indexes, applicationIndexes);
        indexes.add(buildAdditionalIndex());
        return new IndexWrapper(CompositeIndex.create(indexes), deploymentClassLoader, additionalClasses);
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
        index(indexer, Model.class.getName());
        index(indexer, Lock.class.getName());
        index(indexer, All.class.getName());
        index(indexer, Identified.class.getName());
        // Arc built-in beans
        index(indexer, ActivateRequestContextInterceptor.class.getName());
        index(indexer, InjectableRequestContextController.class.getName());
        index(indexer, LockInterceptor.class.getName());
        index(indexer, DefaultAsyncObserverExceptionHandler.class.getName());
        return indexer.complete();
    }

    /**
     * This wrapper is used to index JDK classes on demand.
     */
    static class IndexWrapper implements IndexView {

        private final IndexView index;
        private final ClassLoader deploymentClassLoader;
        final Map<DotName, Optional<ClassInfo>> additionalClasses;

        public IndexWrapper(IndexView index, ClassLoader deploymentClassLoader,
                Map<DotName, Optional<ClassInfo>> additionalClasses) {
            this.index = index;
            this.deploymentClassLoader = deploymentClassLoader;
            this.additionalClasses = additionalClasses;
        }

        @Override
        public Collection<ClassInfo> getKnownClasses() {
            if (additionalClasses.isEmpty()) {
                return index.getKnownClasses();
            }
            List<ClassInfo> all = new ArrayList<>(index.getKnownClasses());
            for (Optional<ClassInfo> additional : additionalClasses.values()) {
                if (additional.isPresent()) {
                    all.add(additional.get());
                }
            }
            return all;
        }

        @Override
        public ClassInfo getClassByName(DotName className) {
            ClassInfo classInfo = IndexClassLookupUtils.getClassByName(index, className, false);
            if (classInfo == null) {
                classInfo = additionalClasses.computeIfAbsent(className, this::computeAdditional).orElse(null);
            }
            return classInfo;
        }

        @Override
        public Collection<ClassInfo> getKnownDirectSubclasses(DotName className) {
            if (additionalClasses.isEmpty()) {
                return index.getKnownDirectSubclasses(className);
            }
            Set<ClassInfo> directSubclasses = new HashSet<ClassInfo>(index.getKnownDirectSubclasses(className));
            for (Optional<ClassInfo> additional : additionalClasses.values()) {
                if (additional.isPresent() && className.equals(additional.get().superName())) {
                    directSubclasses.add(additional.get());
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

        @Override
        public Collection<ClassInfo> getKnownDirectSubinterfaces(DotName interfaceName) {
            if (additionalClasses.isEmpty()) {
                return index.getKnownDirectSubinterfaces(interfaceName);
            }
            Set<ClassInfo> directSubinterfaces = new HashSet<>(index.getKnownDirectSubinterfaces(interfaceName));
            for (Optional<ClassInfo> additional : additionalClasses.values()) {
                if (additional.isPresent() && additional.get().interfaceNames().contains(interfaceName)) {
                    directSubinterfaces.add(additional.get());
                }
            }
            return directSubinterfaces;
        }

        @Override
        public Collection<ClassInfo> getAllKnownSubinterfaces(DotName interfaceName) {
            if (additionalClasses.isEmpty()) {
                return index.getAllKnownSubinterfaces(interfaceName);
            }

            Set<ClassInfo> result = new HashSet<>();

            Queue<DotName> workQueue = new ArrayDeque<>();
            Set<DotName> alreadyProcessed = new HashSet<>();

            workQueue.add(interfaceName);
            while (!workQueue.isEmpty()) {
                DotName iface = workQueue.remove();
                if (!alreadyProcessed.add(iface)) {
                    continue;
                }

                for (ClassInfo directSubinterface : getKnownDirectSubinterfaces(iface)) {
                    result.add(directSubinterface);
                    workQueue.add(directSubinterface.name());
                }
            }

            return result;
        }

        @Override
        public Collection<ClassInfo> getKnownDirectImplementors(DotName className) {
            if (additionalClasses.isEmpty()) {
                return index.getKnownDirectImplementors(className);
            }
            Set<ClassInfo> directImplementors = new HashSet<ClassInfo>(index.getKnownDirectImplementors(className));
            for (Optional<ClassInfo> additional : additionalClasses.values()) {
                if (!additional.isPresent()) {
                    continue;
                }
                for (Type interfaceType : additional.get().interfaceTypes()) {
                    if (className.equals(interfaceType.name())) {
                        directImplementors.add(additional.get());
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

        @Override
        public Collection<AnnotationInstance> getAnnotations(DotName annotationName) {
            return index.getAnnotations(annotationName);
        }

        @Override
        public Collection<AnnotationInstance> getAnnotationsWithRepeatable(DotName annotationName, IndexView index) {
            return this.index.getAnnotationsWithRepeatable(annotationName, index);
        }

        @Override
        public Collection<ModuleInfo> getKnownModules() {
            return this.index.getKnownModules();
        }

        @Override
        public ModuleInfo getModuleByName(DotName moduleName) {
            return this.index.getModuleByName(moduleName);
        }

        @Override
        public Collection<ClassInfo> getKnownUsers(DotName className) {
            return this.index.getKnownUsers(className);
        }

        @Override
        public Collection<ClassInfo> getClassesInPackage(DotName packageName) {
            if (additionalClasses.isEmpty()) {
                return index.getClassesInPackage(packageName);
            }
            Set<ClassInfo> classesInPackage = new HashSet<>(index.getClassesInPackage(packageName));
            for (Optional<ClassInfo> additional : additionalClasses.values()) {
                if (additional.isEmpty()) {
                    continue;
                }
                if (Objects.equals(packageName, additional.get().name().packagePrefixName())) {
                    classesInPackage.add(additional.get());
                }
            }
            return classesInPackage;
        }

        @Override
        public Set<DotName> getSubpackages(DotName packageName) {
            if (additionalClasses.isEmpty()) {
                return index.getSubpackages(packageName);
            }
            Set<DotName> subpackages = new HashSet<>(index.getSubpackages(packageName));
            for (Optional<ClassInfo> additional : additionalClasses.values()) {
                if (additional.isEmpty()) {
                    continue;
                }
                DotName pkg = additional.get().name().packagePrefixName();
                while (pkg != null) {
                    DotName superPkg = pkg.packagePrefixName();
                    if (superPkg != null && superPkg.equals(packageName)) {
                        subpackages.add(pkg);
                    }
                    pkg = superPkg;
                }

            }
            return subpackages;
        }

        private Optional<ClassInfo> computeAdditional(DotName className) {
            LOGGER.debugf("Index: %s", className);
            Indexer indexer = new Indexer();
            if (BeanArchives.index(indexer, className.toString(), deploymentClassLoader)) {
                Index index = indexer.complete();
                return Optional.of(index.getClassByName(className));
            } else {
                // Note that ConcurrentHashMap does not allow null to be used as a value
                return Optional.empty();
            }
        }

    }

    static boolean index(Indexer indexer, String className) {
        return index(indexer, className, BeanArchives.class.getClassLoader());
    }

    static boolean index(Indexer indexer, String className, ClassLoader classLoader) {
        boolean result = false;
        if (Types.isPrimitiveClassName(className) || className.startsWith("[")) {
            // Ignore primitives and arrays
            return false;
        }
        if (isInUnnamedPackage(className)) {
            LOGGER.debugf("Class %s is defined in an unnamed package; skipping indexing", className);
            return false;
        }
        try (InputStream stream = classLoader
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (stream != null) {
                indexer.index(stream);
                result = true;
            } else {
                LOGGER.warnf("Failed to index %s: Class does not exist in ClassLoader %s", className, classLoader);
            }
        } catch (IOException e) {
            LOGGER.warnf(e, "Failed to index %s: %s", className, e.getMessage());
        }
        return result;
    }

    private static boolean isInUnnamedPackage(String className) {
        return !className.contains(".");
    }
}
