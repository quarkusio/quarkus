package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.Map;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

/**
 * Build-time extension point.
 *
 * @author Martin Kouba
 */
public interface BuildExtension {

    static final int DEFAULT_PRIORITY = 1000;

    static int compare(BuildExtension e1, BuildExtension e2) {
        return Integer.compare(e2.getPriority(), e1.getPriority());
    }

    /**
     * Processors with higher priority are called first.
     *
     * @return the priority
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * Initialize self.
     *
     * @param buildContext
     * @return {@code true} if the extension should be put into service, @{code false} otherwise
     */
    default boolean initialize(BuildContext buildContext) {
        return true;
    }

    interface BuildContext {

        <V> V get(Key<V> key);

        <V> V put(Key<V> key, V value);

    }

    interface Key<T> {

        // Built-in keys
        static String BUILT_IN_PREFIX = BuildExtension.class.getPackage().getName() + ".";
        static Key<IndexView> INDEX = new SimpleKey<>(BUILT_IN_PREFIX + "index");
        static Key<Collection<InjectionPointInfo>> INJECTION_POINTS = new SimpleKey<>(BUILT_IN_PREFIX + "injectionPoints");
        static Key<Collection<BeanInfo>> BEANS = new SimpleKey<>(BUILT_IN_PREFIX + "beans");
        static Key<Collection<BeanInfo>> REMOVED_BEANS = new SimpleKey<>(BUILT_IN_PREFIX + "removedBeans");
        static Key<Collection<ObserverInfo>> OBSERVERS = new SimpleKey<>(BUILT_IN_PREFIX + "observers");
        static Key<Collection<InterceptorInfo>> INTERCEPTORS = new SimpleKey<>(BUILT_IN_PREFIX + "interceptors");
        static Key<AnnotationStore> ANNOTATION_STORE = new SimpleKey<>(BUILT_IN_PREFIX + "annotationStore");
        static Key<Collection<ScopeInfo>> SCOPES = new SimpleKey<>(BUILT_IN_PREFIX + "scopes");
        static Key<Map<DotName, ClassInfo>> QUALIFIERS = new SimpleKey<>(BUILT_IN_PREFIX + "qualifiers");
        static Key<Map<DotName, ClassInfo>> INTERCEPTOR_BINDINGS = new SimpleKey<>(BUILT_IN_PREFIX + "interceptorBindings");
        static Key<Map<DotName, StereotypeInfo>> STEREOTYPES = new SimpleKey<>(BUILT_IN_PREFIX + "stereotypes");

        String asString();
    }

    public static class SimpleKey<V> implements Key<V> {

        private final String str;

        public SimpleKey(String str) {
            this.str = str;
        }

        @Override
        public String asString() {
            return str;
        }

    }

}
