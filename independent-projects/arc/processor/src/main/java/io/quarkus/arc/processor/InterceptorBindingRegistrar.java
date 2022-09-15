package io.quarkus.arc.processor;

import jakarta.enterprise.util.Nonbinding;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jboss.jandex.DotName;

public interface InterceptorBindingRegistrar extends BuildExtension {

    /**
     * @return the list of additional interceptor bindings
     */
    default List<InterceptorBinding> getAdditionalBindings() {
        return Collections.emptyList();
    }

    /**
     * Represents an additional interceptor binding definition. The name is used to identify an annotation class and the
     * {@link #isNonbinding(String)} method is used to match annotation values that should be considered non-binding as defined
     * by the
     * CDI spec.
     */
    interface InterceptorBinding {

        static InterceptorBinding of(Class<? extends Annotation> clazz) {
            return of(DotName.createSimple(clazz.getName()));
        }

        static InterceptorBinding of(DotName name) {
            return of(name, member -> false);
        }

        static InterceptorBinding of(Class<? extends Annotation> clazz, Predicate<String> predicate) {
            return of(DotName.createSimple(clazz.getName()), predicate);
        }

        static InterceptorBinding of(DotName name, Predicate<String> predicate) {
            return new InterceptorBinding() {

                @Override
                public boolean isNonbinding(String memberName) {
                    return predicate.test(memberName);
                }

                @Override
                public DotName getName() {
                    return name;
                }
            };
        }

        static InterceptorBinding of(DotName name, Set<String> nonbinding) {
            return of(name, nonbinding::contains);
        }

        static InterceptorBinding of(Class<? extends Annotation> clazz, Set<String> nonbinding) {
            return of(clazz, nonbinding::contains);
        }

        /**
         *
         * @return the name of the annotation that should be considered an interceptor binding
         */
        DotName getName();

        /**
         * @return {@code true} if the annotation member with the given name should be considered non-binding, {@code false}
         *         otherwise
         * @see Nonbinding
         */
        boolean isNonbinding(String memberName);

    }

}
