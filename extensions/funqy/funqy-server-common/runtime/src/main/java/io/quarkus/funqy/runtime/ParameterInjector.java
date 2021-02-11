package io.quarkus.funqy.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.quarkus.funqy.Context;

public class ParameterInjector {
    public static ValueInjector createInjector(Type type, Class clz, Annotation[] annotations) {
        if (annotations != null) {
            for (Annotation ann : annotations) {
                if (ann.annotationType().equals(Context.class)) {
                    return new ContextValueInjector(clz);
                }
            }
        }
        return new InputValueInjector(clz);

    }
}
