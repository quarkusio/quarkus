package io.quarkus.deployment.builditem.nativeimage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final Predicate<DotName> ignorePredicate;

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

    public static class DefaultIgnorePredicate implements Predicate<DotName> {

        public static final DefaultIgnorePredicate INSTANCE = new DefaultIgnorePredicate();

        private static final List<String> DEFAULT_IGNORED_PACKAGES = Arrays.asList("java.", "io.reactivex.",
                "org.reactivestreams.", "org.slf4j.");
        // if this gets more complicated we will need to move to some tree like structure
        static final Set<String> WHITELISTED_FROM_IGNORED_PACKAGES = new HashSet<>(
                Arrays.asList("java.math.BigDecimal", "java.math.BigInteger"));

        static final List<String> PRIMITIVE = Arrays.asList("boolean", "byte",
                "char", "short", "int", "long", "float", "double");

        @Override
        public boolean test(DotName dotName) {
            String name = dotName.toString();
            if (PRIMITIVE.contains(name)) {
                return true;
            }
            for (String containerPackageName : DEFAULT_IGNORED_PACKAGES) {
                if (name.startsWith(containerPackageName)) {
                    return !WHITELISTED_FROM_IGNORED_PACKAGES.contains(name);
                }
            }
            return false;
        }

    }

    public static class IgnoreWhiteListedPredicate implements Predicate<DotName> {

        public static IgnoreWhiteListedPredicate INSTANCE = new IgnoreWhiteListedPredicate();

        @Override
        public boolean test(DotName dotName) {
            return DefaultIgnorePredicate.WHITELISTED_FROM_IGNORED_PACKAGES.contains(dotName.toString());
        }
    }

}
