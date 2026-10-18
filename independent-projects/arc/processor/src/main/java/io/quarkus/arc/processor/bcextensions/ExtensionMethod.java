package io.quarkus.arc.processor.bcextensions;

import java.util.List;

class ExtensionMethod {
    // right now, `extensionClass` is always `method.declaringClass()`,
    // but if we allow inheriting extension methods, that will change
    final org.jboss.jandex.ClassInfo extensionClass;
    final org.jboss.jandex.MethodInfo jandex;

    ExtensionMethod(org.jboss.jandex.MethodInfo jandexMethod) {
        this.extensionClass = jandexMethod.declaringClass();
        this.jandex = jandexMethod;
    }

    String name() {
        return jandex.name();
    }

    int parametersCount() {
        return jandex.parametersCount();
    }

    List<org.jboss.jandex.Type> parameterTypes() {
        return jandex.parameterTypes();
    }

    org.jboss.jandex.Type parameterType(int index) {
        return jandex.parameterType(index);
    }

    @Override
    public String toString() {
        return jandex.declaringClass().simpleName() + "." + jandex.name() + "()";
    }
}
