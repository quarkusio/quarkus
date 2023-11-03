package io.quarkus.arc.processor;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

public interface ReflectionRegistration {

    void registerMethod(String declaringClass, String name, String... params);

    void registerMethod(MethodInfo methodInfo);

    void registerField(FieldInfo fieldInfo);

    /**
     * Register the client proxy for the given bean class if it's needed.
     *
     * @param beanClassName
     * @param clientProxyName
     */
    default void registerClientProxy(DotName beanClassName, String clientProxyName) {
    }

    /**
     * Register the intercepted subclass for the given bean class if it's needed.
     *
     * @param beanClassName
     * @param subclassName
     */
    default void registerSubclass(DotName beanClassName, String subclassName) {
    }

    ReflectionRegistration NOOP = new ReflectionRegistration() {

        @Override
        public void registerMethod(String declaringClass, String name, String... params) {
        }

        @Override
        public void registerMethod(MethodInfo methodInfo) {
        }

        @Override
        public void registerField(FieldInfo fieldInfo) {
        }
    };
}
