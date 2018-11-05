package org.jboss.protean.arc.processor;

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
     * Initialize self. Processors are initialized before a bean deployment is constructed.
     *
     * @param index
     * @param contextData
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
        static Key<IndexView> INDEX = new SimpleKey<IndexView>(BUILT_IN_PREFIX + "index");
        static Key<IndexView> INJECTION_POINTS = new SimpleKey<IndexView>(BUILT_IN_PREFIX + "injectionPoints");
        static Key<IndexView> ANNOTATION_STORE = new SimpleKey<IndexView>(BUILT_IN_PREFIX + "annotationStore");

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
