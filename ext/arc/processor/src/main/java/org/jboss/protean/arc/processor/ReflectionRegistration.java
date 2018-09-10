package org.jboss.protean.arc.processor;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

public interface ReflectionRegistration {

    void registerMethod(MethodInfo methodInfo);

    void registerField(FieldInfo fieldInfo);

    ReflectionRegistration NOOP = new ReflectionRegistration() {
        @Override
        public void registerMethod(MethodInfo methodInfo) {

        }

        @Override
        public void registerField(FieldInfo fieldInfo) {

        }
    };
}
