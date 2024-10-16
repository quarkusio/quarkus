package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

import io.quarkus.arc.processor.Annotations;

abstract class SyntheticComponentBuilderBase<THIS extends SyntheticComponentBuilderBase<THIS>> {
    Map<String, Object> params = new HashMap<>();

    abstract THIS self();

    public THIS withParam(String key, boolean value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, boolean[] value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, int value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, int[] value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, long value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, long[] value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, double value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, double[] value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, String value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, String[] value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, Enum<?> value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, Enum<?>[] value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, Class<?> value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, ClassInfo value) {
        params.put(key, ((ClassInfoImpl) value).jandexDeclaration);
        return self();
    }

    public THIS withParam(String key, Class<?>[] value) {
        params.put(key, value);
        return self();
    }

    public THIS withParam(String key, ClassInfo[] value) {
        org.jboss.jandex.ClassInfo[] jandexValues = new org.jboss.jandex.ClassInfo[value.length];
        for (int i = 0; i < value.length; i++) {
            jandexValues[i] = ((ClassInfoImpl) value[i]).jandexDeclaration;
        }
        params.put(key, jandexValues);
        return self();
    }

    public THIS withParam(String key, AnnotationInfo value) {
        params.put(key, ((AnnotationInfoImpl) value).jandexAnnotation);
        return self();
    }

    public THIS withParam(String key, Annotation value) {
        params.put(key, Annotations.jandexAnnotation(value));
        return self();
    }

    public THIS withParam(String key, AnnotationInfo[] value) {
        org.jboss.jandex.AnnotationInstance[] jandexValues = new org.jboss.jandex.AnnotationInstance[value.length];
        for (int i = 0; i < value.length; i++) {
            jandexValues[i] = ((AnnotationInfoImpl) value[i]).jandexAnnotation;
        }
        params.put(key, jandexValues);
        return self();
    }

    public THIS withParam(String key, Annotation[] value) {
        org.jboss.jandex.AnnotationInstance[] jandexValues = new org.jboss.jandex.AnnotationInstance[value.length];
        for (int i = 0; i < value.length; i++) {
            jandexValues[i] = Annotations.jandexAnnotation(value[i]);
        }
        params.put(key, jandexValues);
        return self();
    }

    public THIS withParam(String key, InvokerInfo value) {
        io.quarkus.arc.processor.InvokerInfo arcValue = ((InvokerInfoImpl) value).arcInvokerInfo;
        params.put(key, arcValue);
        return self();
    }

    public THIS withParam(String key, InvokerInfo[] value) {
        io.quarkus.arc.processor.InvokerInfo[] arcValues = new io.quarkus.arc.processor.InvokerInfo[value.length];
        for (int i = 0; i < value.length; i++) {
            arcValues[i] = ((InvokerInfoImpl) value[i]).arcInvokerInfo;
        }
        params.put(key, arcValues);
        return self();
    }

}
