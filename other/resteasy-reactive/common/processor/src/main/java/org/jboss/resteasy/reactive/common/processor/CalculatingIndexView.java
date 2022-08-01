package org.jboss.resteasy.reactive.common.processor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.ModuleInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

public class CalculatingIndexView implements IndexView {

    private static final Logger LOGGER = Logger.getLogger(CalculatingIndexView.class);

    private final IndexView index;
    private final ClassLoader classLoader;
    final Map<DotName, Optional<ClassInfo>> additionalClasses;

    public CalculatingIndexView(IndexView index, ClassLoader classLoader, Map<DotName, Optional<ClassInfo>> additionalClasses) {
        this.index = index;
        this.classLoader = classLoader;
        this.additionalClasses = additionalClasses;
    }

    @Override
    public Collection<ClassInfo> getKnownClasses() {
        if (additionalClasses.isEmpty()) {
            return index.getKnownClasses();
        }
        Collection<ClassInfo> known = index.getKnownClasses();
        Collection<ClassInfo> additional = additionalClasses.values().stream().filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        List<ClassInfo> all = new ArrayList<>(known.size() + additional.size());
        all.addAll(known);
        all.addAll(additional);
        return all;
    }

    @Override
    public ClassInfo getClassByName(DotName className) {
        ClassInfo classInfo = index.getClassByName(className);
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

    private Optional<ClassInfo> computeAdditional(DotName className) {
        LOGGER.debugf("Index: %s", className);
        Indexer indexer = new Indexer();
        if (index(indexer, className.toString(), classLoader)) {
            Index index = indexer.complete();
            return Optional.of(index.getClassByName(className));
        } else {
            // Note that ConcurrentHashMap does not allow null to be used as a value
            return Optional.empty();
        }
    }

    static boolean index(Indexer indexer, String className, ClassLoader classLoader) {
        boolean result = false;
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
}
