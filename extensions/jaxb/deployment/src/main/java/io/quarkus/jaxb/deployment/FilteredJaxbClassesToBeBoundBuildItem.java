package io.quarkus.jaxb.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.jaxb.deployment.utils.JaxbType;

/**
 * List of classes to be bound in the JAXB context. Aggregates all classes passed via
 * {@link JaxbClassesToBeBoundBuildItem}. All class names excluded via {@code quarkus.jaxb.exclude-classes} are not
 * present in this list.
 */
public final class FilteredJaxbClassesToBeBoundBuildItem extends SimpleBuildItem {

    private final List<Class<?>> classes;

    public static Builder builder() {
        return new Builder();
    }

    private FilteredJaxbClassesToBeBoundBuildItem(List<Class<?>> classes) {
        this.classes = classes;
    }

    public List<Class<?>> getClasses() {
        return new ArrayList<>(classes);
    }

    public static class Builder {
        private final Set<String> classNames = new LinkedHashSet<>();
        private final Set<String> classNameExcludes = new LinkedHashSet<>();

        public Builder classNameExcludes(Collection<String> classNameExcludes) {
            for (String className : classNameExcludes) {
                this.classNameExcludes.add(className);
            }
            return this;
        }

        public Builder classNames(Collection<String> classNames) {
            for (String className : classNames) {
                this.classNames.add(className);
            }
            return this;
        }

        public FilteredJaxbClassesToBeBoundBuildItem build() {
            final List<Class<?>> classes = classNames.stream()
                    .filter(className -> !this.classNameExcludes.contains(className))
                    .map(FilteredJaxbClassesToBeBoundBuildItem::getClassByName)
                    .filter(JaxbType::isValidType)
                    .collect(Collectors.toList());

            return new FilteredJaxbClassesToBeBoundBuildItem(classes);
        }
    }

    private static Class<?> getClassByName(String name) {
        try {
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
