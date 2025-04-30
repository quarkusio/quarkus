package io.quarkus.arc.processor.bcextensions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.interceptor.Interceptor;

import org.jboss.jandex.DotName;

// only this class uses reflection, everything else in this package is reflection-free
class ExtensionInvoker {
    private final Map<String, Class<?>> extensionClasses = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> extensionClassInstances = new ConcurrentHashMap<>();

    private final org.jboss.jandex.IndexView extensionsIndex;

    ExtensionInvoker(List<BuildCompatibleExtension> extensions) {
        // indexing the extension classes just to be able to use Jandex to reflect on extension methods
        // TODO inherit extension methods from superclasses?
        org.jboss.jandex.Indexer extensionsIndexer = new org.jboss.jandex.Indexer();
        List<BuildCompatibleExtension> allExtensions = new ArrayList<>(extensions);
        for (BuildCompatibleExtension extension : ServiceLoader.load(BuildCompatibleExtension.class)) {
            allExtensions.add(extension);
        }
        for (BuildCompatibleExtension extension : allExtensions) {
            Class<? extends BuildCompatibleExtension> extensionClass = extension.getClass();
            extensionClasses.put(extensionClass.getName(), extensionClass);
            extensionClassInstances.put(extensionClass, extension);
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    extensionClass.getName().replace('.', '/').concat(".class"))) {
                extensionsIndexer.index(stream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        extensionsIndex = extensionsIndexer.complete();
    }

    List<ExtensionMethod> findExtensionMethods(DotName annotation) {
        return extensionsIndex.getAllKnownImplementors(DotNames.BUILD_COMPATIBLE_EXTENSION)
                .stream()
                .flatMap(it -> it.annotationsMap()
                        .getOrDefault(annotation, Collections.emptyList())
                        .stream()
                        .filter(ann -> ann.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD)
                        .map(ann -> ann.target().asMethod()))
                .sorted((m1, m2) -> {
                    if (m1 == m2) {
                        // at this particular point, two different org.jboss.jandex.MethodInfo instances are never equal
                        return 0;
                    }

                    int p1 = getExtensionMethodPriority(m1);
                    int p2 = getExtensionMethodPriority(m2);

                    // must _not_ return 0 if priorities are equal, because that isn't consistent
                    // with the `equals` method (see also above)
                    return p1 < p2 ? -1 : 1;
                })
                .map(ExtensionMethod::new)
                .toList();
    }

    private int getExtensionMethodPriority(org.jboss.jandex.MethodInfo method) {
        org.jboss.jandex.AnnotationInstance priority = method.declaredAnnotation(DotNames.PRIORITY);
        if (priority != null) {
            return priority.value().asInt();
        }
        return Interceptor.Priority.APPLICATION + 500;
    }

    void callExtensionMethod(ExtensionMethod method, List<Object> arguments)
            throws ReflectiveOperationException {

        Class<?>[] parameterTypes = new Class[arguments.size()];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = org.jboss.jandex.JandexReflection.loadRawType(method.parameterType(i));
        }

        Class<?> extensionClass = extensionClasses.get(method.extensionClass.name().toString());
        Object extensionClassInstance = extensionClassInstances.get(extensionClass);

        Method methodReflective = extensionClass.getDeclaredMethod(method.name(), parameterTypes);
        methodReflective.setAccessible(true);
        methodReflective.invoke(extensionClassInstance, arguments.toArray());
    }

    void invalidate() {
        extensionClasses.clear();
        extensionClassInstances.clear();
    }

    /**
     * @return {@code true} if no {@link BuildCompatibleExtension} was found, {@code false} otherwise
     */
    boolean isEmpty() {
        return extensionClasses.isEmpty();
    }
}
