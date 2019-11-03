package io.quarkus.deployment.builditem.nativeimage;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Attempts to register a complete type hierarchy for reflection.
 * <p>
 * This is intended to be used to register types that are going to be serialized,
 * e.g. by Jackson or some other JSON mapper.
 * <p>
 * This will do 'smart discovery' and in addition to registering the type itself it will also attempt to
 * register the following:
 * <p>
 * - Superclasses
 * - Component types of collections
 * - Types used in bean properties if (if method reflection is enabled)
 * - Field types (if field reflection is enabled)
 * <p>
 * This discovery is applied recursively, so any additional types that are registered will also have their dependencies
 * discovered
 */
public final class ReflectiveHierarchyBuildItem extends MultiBuildItem {

    private final Type type;
    private IndexView index;
    private Predicate<DotName> ignorePredicate;

    public ReflectiveHierarchyBuildItem(Type type) {
        this(type, DefaultIgnorePredicate.INSTANCE);
    }

    public ReflectiveHierarchyBuildItem(Type type, IndexView index) {
        this(type, index, DefaultIgnorePredicate.INSTANCE);
    }

    public ReflectiveHierarchyBuildItem(Type type, Predicate<DotName> ignorePredicate) {
        this.type = type;
        this.ignorePredicate = ignorePredicate;
    }

    public ReflectiveHierarchyBuildItem(Type type, IndexView index, Predicate<DotName> ignorePredicate) {
        this.type = type;
        this.index = index;
        this.ignorePredicate = ignorePredicate;
    }

    public Type getType() {
        return type;
    }

    public IndexView getIndex() {
        return index;
    }

    public Predicate<DotName> getIgnorePredicate() {
        return ignorePredicate;
    }

    private static class DefaultIgnorePredicate implements Predicate<DotName> {

        private static final DefaultIgnorePredicate INSTANCE = new DefaultIgnorePredicate();

        private static final List<String> DEFAULT_IGNORED_PACKAGES = Arrays.asList("java.", "io.reactivex.",
                "org.reactivestreams.");

        @Override
        public boolean test(DotName name) {
            return isInContainerPackage(name.toString());
        }

        private boolean isInContainerPackage(String name) {
            for (String containerPackageName : DEFAULT_IGNORED_PACKAGES) {
                if (name.startsWith(containerPackageName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
