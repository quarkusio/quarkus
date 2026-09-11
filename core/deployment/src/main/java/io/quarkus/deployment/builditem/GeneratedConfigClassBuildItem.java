package io.quarkus.deployment.builditem;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;

public final class GeneratedConfigClassBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    private final Map<Class<?>, ConfigClassImplementation> elements;
    private final Set<DotName> interfaces;
    private final Set<DotName> implementations;

    public GeneratedConfigClassBuildItem(
            Class<?> configClass,
            Map<Class<?>, ConfigClassImplementation> elements,
            Set<DotName> interfaces,
            Set<DotName> implementations) {
        this.configClass = configClass;
        this.elements = unmodifiableMap(elements);
        this.interfaces = unmodifiableSet(interfaces);
        this.implementations = unmodifiableSet(implementations);
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public Map<Class<?>, ConfigClassImplementation> getElements() {
        return elements;
    }

    public Set<DotName> getInterfaces() {
        return interfaces;
    }

    public Set<DotName> getImplementations() {
        return implementations;
    }

    public static GeneratedConfigClassBuildItem of(final DotName configClass) {
        return of(loadClass(configClass.toString()));
    }

    public static GeneratedConfigClassBuildItem of(final Class<?> configClass) {
        Set<GeneratedConfigClass> generatedClasses = ConfigMappingLoader.getGeneratedConfigClasses(configClass);
        boolean isApplicationClass = QuarkusClassLoader.isApplicationClass(configClass.getName());

        Map<Class<?>, ConfigClassImplementation> elements = new HashMap<>();
        Set<DotName> interfaces = new HashSet<>();
        Set<DotName> implementations = new HashSet<>();
        for (GeneratedConfigClass generatedClass : generatedClasses) {
            elements.putIfAbsent(generatedClass.getParent(), new ConfigClassImplementation(generatedClass, isApplicationClass));
            if (generatedClass.getParent().equals(generatedClass.getInterfaceType())) {
                interfaces.add(DotName.createSimple(generatedClass.getInterfaceType()));
                implementations.add(DotName.createSimple(generatedClass.getClassName()));
            }
        }
        return new GeneratedConfigClassBuildItem(configClass, elements, interfaces, implementations);
    }

    private static Class<?> loadClass(final String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The class (" + name + ") cannot be created during deployment.", e);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GeneratedConfigClassBuildItem that = (GeneratedConfigClassBuildItem) o;
        return configClass.equals(that.configClass);
    }

    @Override
    public int hashCode() {
        return configClass.hashCode();
    }

    public static class ConfigClassImplementation {
        private final GeneratedConfigClass generatedConfigClass;
        private final boolean isApplicationClass;

        ConfigClassImplementation(final GeneratedConfigClass generatedConfigClass, final boolean isApplicationClass) {
            this.generatedConfigClass = generatedConfigClass;
            this.isApplicationClass = isApplicationClass;
        }

        public boolean isApplicationClass() {
            return isApplicationClass;
        }

        public String getName() {
            return generatedConfigClass.getClassName();
        }

        public byte[] getBytes() {
            return generatedConfigClass.generateClassBytes();
        }
    }
}
