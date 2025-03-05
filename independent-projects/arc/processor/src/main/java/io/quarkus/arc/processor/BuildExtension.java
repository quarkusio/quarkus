package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.BuildExtension.SimpleKey.simpleBuiltIn;

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
     * @return {@code true} if the extension should be put into service, {@code false} otherwise
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
        static Key<IndexView> INDEX = simpleBuiltIn("index");
        static Key<Collection<InjectionPointInfo>> INJECTION_POINTS = simpleBuiltIn("injectionPoints");
        static Key<Collection<BeanInfo>> BEANS = simpleBuiltIn("beans");
        static Key<Collection<BeanInfo>> REMOVED_BEANS = simpleBuiltIn("removedBeans");
        static Key<Collection<ObserverInfo>> OBSERVERS = simpleBuiltIn("observers");
        static Key<Collection<InterceptorInfo>> INTERCEPTORS = simpleBuiltIn("interceptors");
        static Key<Collection<InterceptorInfo>> REMOVED_INTERCEPTORS = simpleBuiltIn("removedInterceptors");
        static Key<Collection<DecoratorInfo>> DECORATORS = simpleBuiltIn("decorators");
        static Key<Collection<DecoratorInfo>> REMOVED_DECORATORS = simpleBuiltIn("removedDecorators");
        static Key<AnnotationStore> ANNOTATION_STORE = simpleBuiltIn("annotationStore");
        static Key<Collection<ScopeInfo>> SCOPES = simpleBuiltIn("scopes");
        static Key<Map<DotName, ClassInfo>> QUALIFIERS = simpleBuiltIn("qualifiers");
        static Key<Map<DotName, ClassInfo>> INTERCEPTOR_BINDINGS = simpleBuiltIn("interceptorBindings");
        static Key<Map<DotName, StereotypeInfo>> STEREOTYPES = simpleBuiltIn("stereotypes");
        static Key<InvokerFactory> INVOKER_FACTORY = simpleBuiltIn("invokerFactory");
        static Key<BeanDeployment> DEPLOYMENT = simpleBuiltIn("deployment");

        String asString();
    }

    public static class SimpleKey<V> implements Key<V> {

        static <V> SimpleKey<V> simpleBuiltIn(String val) {
            return new SimpleKey<>(BUILT_IN_PREFIX + val);
        }

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
