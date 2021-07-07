package io.quarkus.deployment.builditem.nativeimage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
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

    private static final String UNKNOWN_SOURCE = "<unknown>";

    private final Type type;
    private final IndexView index;
    private final Predicate<DotName> ignoreTypePredicate;
    private final Predicate<FieldInfo> ignoreFieldPredicate;
    private final Predicate<MethodInfo> ignoreMethodPredicate;
    private final String source;
    private final boolean serialization;

    /**
     * @deprecated Use the Builder instead.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type) {
        this(type, DefaultIgnoreTypePredicate.INSTANCE);
    }

    /**
     * @deprecated Use the Builder instead and provide a source for easy debugging.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type, IndexView index) {
        this(type, index, DefaultIgnoreTypePredicate.INSTANCE);
    }

    /**
     * @deprecated Use the Builder instead and provide a source for easy debugging.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type, Predicate<DotName> ignoreTypePredicate) {
        this(type, ignoreTypePredicate, UNKNOWN_SOURCE);
    }

    /**
     * @deprecated Use the Builder instead and provide a source for easy debugging.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type, IndexView index, Predicate<DotName> ignoreTypePredicate) {
        this(type, index, ignoreTypePredicate, UNKNOWN_SOURCE);
    }

    /**
     * @deprecated Use the Builder instead and provide a source for easy debugging.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type, String source) {
        this(type, DefaultIgnoreTypePredicate.INSTANCE, source);
    }

    /**
     * @deprecated Use the Builder instead and provide a source for easy debugging.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type, IndexView index, String source) {
        this(type, index, DefaultIgnoreTypePredicate.INSTANCE, source);
    }

    /**
     * @deprecated Use the Builder instead and provide a source for easy debugging.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type, Predicate<DotName> ignoreTypePredicate, String source) {
        this(type, null, ignoreTypePredicate, source);
    }

    /**
     * @deprecated Use the Builder instead and provide a source for easy debugging.
     */
    @Deprecated
    public ReflectiveHierarchyBuildItem(Type type, IndexView index, Predicate<DotName> ignoreTypePredicate, String source) {
        this(type, index, ignoreTypePredicate, DefaultIgnoreFieldPredicate.INSTANCE, DefaultIgnoreMethodPredicate.INSTANCE,
                source, false);
    }

    private ReflectiveHierarchyBuildItem(Type type, IndexView index, Predicate<DotName> ignoreTypePredicate,
            Predicate<FieldInfo> ignoreFieldPredicate, Predicate<MethodInfo> ignoreMethodPredicate, String source,
            boolean serialization) {
        this.type = type;
        this.index = index;
        this.ignoreTypePredicate = ignoreTypePredicate;
        this.ignoreFieldPredicate = ignoreFieldPredicate;
        this.ignoreMethodPredicate = ignoreMethodPredicate;
        this.source = source;
        this.serialization = serialization;
    }

    public Type getType() {
        return type;
    }

    public IndexView getIndex() {
        return index;
    }

    public Predicate<DotName> getIgnoreTypePredicate() {
        return ignoreTypePredicate;
    }

    public Predicate<FieldInfo> getIgnoreFieldPredicate() {
        return ignoreFieldPredicate;
    }

    public Predicate<MethodInfo> getIgnoreMethodPredicate() {
        return ignoreMethodPredicate;
    }

    public boolean hasSource() {
        return source != null;
    }

    public boolean isSerialization() {
        return serialization;
    }

    public String getSource() {
        return source;
    }

    public static class Builder {

        private Type type;
        private IndexView index;
        private Predicate<DotName> ignoreTypePredicate = DefaultIgnoreTypePredicate.INSTANCE;
        private Predicate<FieldInfo> ignoreFieldPredicate = DefaultIgnoreFieldPredicate.INSTANCE;
        private Predicate<MethodInfo> ignoreMethodPredicate = DefaultIgnoreMethodPredicate.INSTANCE;
        private String source = UNKNOWN_SOURCE;
        private boolean serialization;

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder index(IndexView index) {
            this.index = index;
            return this;
        }

        public Builder ignoreTypePredicate(Predicate<DotName> ignoreTypePredicate) {
            this.ignoreTypePredicate = ignoreTypePredicate;
            return this;
        }

        public Builder ignoreFieldPredicate(Predicate<FieldInfo> ignoreFieldPredicate) {
            this.ignoreFieldPredicate = ignoreFieldPredicate;
            return this;
        }

        public Builder ignoreMethodPredicate(Predicate<MethodInfo> ignoreMethodPredicate) {
            this.ignoreMethodPredicate = ignoreMethodPredicate;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder serialization(boolean serialization) {
            this.serialization = serialization;
            return this;
        }

        public ReflectiveHierarchyBuildItem build() {
            return new ReflectiveHierarchyBuildItem(type, index, ignoreTypePredicate, ignoreFieldPredicate,
                    ignoreMethodPredicate, source, serialization);
        }
    }

    public static class DefaultIgnoreTypePredicate implements Predicate<DotName> {

        public static final DefaultIgnoreTypePredicate INSTANCE = new DefaultIgnoreTypePredicate();

        private static final List<String> DEFAULT_IGNORED_PACKAGES = Arrays.asList("java.", "io.reactivex.",
                "org.reactivestreams.", "org.slf4j.", "javax.json.", "com.fasterxml.jackson.databind.",
                "io.vertx.core.json.");
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
            return DefaultIgnoreTypePredicate.WHITELISTED_FROM_IGNORED_PACKAGES.contains(dotName.toString());
        }
    }

    public static class DefaultIgnoreFieldPredicate implements Predicate<FieldInfo> {

        public static DefaultIgnoreFieldPredicate INSTANCE = new DefaultIgnoreFieldPredicate();

        @Override
        public boolean test(FieldInfo fieldInfo) {
            return false;
        }
    }

    public static class DefaultIgnoreMethodPredicate implements Predicate<MethodInfo> {

        public static DefaultIgnoreMethodPredicate INSTANCE = new DefaultIgnoreMethodPredicate();

        @Override
        public boolean test(MethodInfo methodInfo) {
            return false;
        }
    }

}
