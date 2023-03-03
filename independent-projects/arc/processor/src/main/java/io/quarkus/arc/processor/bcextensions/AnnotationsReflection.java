package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import org.jboss.jandex.DotName;

class AnnotationsReflection {
    static org.jboss.jandex.AnnotationInstance jandexAnnotation(Annotation annotation) {
        Class<? extends Annotation> annotationType = annotationType(annotation);

        DotName name = DotName.createSimple(annotationType.getName());
        org.jboss.jandex.AnnotationValue[] jandexAnnotationValues = jandexAnnotationValues(
                (Class<Annotation>) annotationType, annotation);

        return org.jboss.jandex.AnnotationInstance.create(name, null, jandexAnnotationValues);
    }

    private static Class<? extends Annotation> annotationType(Annotation annotation) {
        Class<? extends Annotation> annotationType = null;

        Queue<Class<?>> candidates = new ArrayDeque<>();
        candidates.add(annotation.getClass());
        while (!candidates.isEmpty()) {
            Class<?> candidate = candidates.remove();

            if (candidate.isAnnotation()) {
                annotationType = (Class<? extends Annotation>) candidate;
                break;
            }

            Collections.addAll(candidates, candidate.getInterfaces());
        }

        if (annotationType == null) {
            throw new IllegalArgumentException("Not an annotation: " + annotation);
        }

        return annotationType;
    }

    private static <A extends Annotation> org.jboss.jandex.AnnotationValue[] jandexAnnotationValues(
            Class<A> annotationType, A annotationInstance) {
        List<org.jboss.jandex.AnnotationValue> result = new ArrayList<>();
        for (Method member : annotationType.getDeclaredMethods()) {
            try {
                // annotation types do not necessarily have to be public (if the annotation type
                // and the build compatible extension class reside in the same package)
                if (!member.canAccess(annotationInstance)) {
                    member.setAccessible(true);
                }
                result.add(jandexAnnotationValue(member.getName(), member.invoke(annotationInstance)));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return result.toArray(new org.jboss.jandex.AnnotationValue[0]);
    }

    private static org.jboss.jandex.AnnotationValue jandexAnnotationValue(String name, Object value) {
        if (value instanceof Boolean) {
            return org.jboss.jandex.AnnotationValue.createBooleanValue(name, (Boolean) value);
        } else if (value instanceof Byte) {
            return org.jboss.jandex.AnnotationValue.createByteValue(name, (Byte) value);
        } else if (value instanceof Short) {
            return org.jboss.jandex.AnnotationValue.createShortValue(name, (Short) value);
        } else if (value instanceof Integer) {
            return org.jboss.jandex.AnnotationValue.createIntegerValue(name, (Integer) value);
        } else if (value instanceof Long) {
            return org.jboss.jandex.AnnotationValue.createLongValue(name, (Long) value);
        } else if (value instanceof Float) {
            return org.jboss.jandex.AnnotationValue.createFloatValue(name, (Float) value);
        } else if (value instanceof Double) {
            return org.jboss.jandex.AnnotationValue.createDoubleValue(name, (Double) value);
        } else if (value instanceof Character) {
            return org.jboss.jandex.AnnotationValue.createCharacterValue(name, (Character) value);
        } else if (value instanceof String) {
            return org.jboss.jandex.AnnotationValue.createStringValue(name, (String) value);
        } else if (value instanceof Enum) {
            return org.jboss.jandex.AnnotationValue.createEnumValue(name,
                    DotName.createSimple(((Enum<?>) value).getDeclaringClass().getName()), ((Enum<?>) value).name());
        } else if (value instanceof Class) {
            return org.jboss.jandex.AnnotationValue.createClassValue(name, TypesReflection.jandexType((Class<?>) value));
        } else if (value.getClass().isAnnotation()) {
            Class<? extends Annotation> annotationType = annotationType((Annotation) value);
            org.jboss.jandex.AnnotationValue[] jandexAnnotationValues = jandexAnnotationValues(
                    (Class<Annotation>) annotationType, (Annotation) value);
            org.jboss.jandex.AnnotationInstance jandexAnnotation = org.jboss.jandex.AnnotationInstance.create(
                    DotName.createSimple(annotationType.getName()), null, jandexAnnotationValues);
            return org.jboss.jandex.AnnotationValue.createNestedAnnotationValue(name, jandexAnnotation);
        } else if (value.getClass().isArray()) {
            org.jboss.jandex.AnnotationValue[] jandexAnnotationValues = Arrays.stream(boxArray(value))
                    .map(it -> jandexAnnotationValue(name, it))
                    .toArray(org.jboss.jandex.AnnotationValue[]::new);
            return org.jboss.jandex.AnnotationValue.createArrayValue(name, jandexAnnotationValues);
        } else {
            throw new IllegalArgumentException("Unknown annotation attribute value: " + value);
        }
    }

    private static Object[] boxArray(Object value) {
        if (value instanceof boolean[]) {
            boolean[] primitiveArray = (boolean[]) value;
            Object[] boxedArray = new Boolean[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof byte[]) {
            byte[] primitiveArray = (byte[]) value;
            Object[] boxedArray = new Byte[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof short[]) {
            short[] primitiveArray = (short[]) value;
            Object[] boxedArray = new Short[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof int[]) {
            int[] primitiveArray = (int[]) value;
            Object[] boxedArray = new Integer[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof long[]) {
            long[] primitiveArray = (long[]) value;
            Object[] boxedArray = new Long[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof float[]) {
            float[] primitiveArray = (float[]) value;
            Object[] boxedArray = new Float[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof double[]) {
            double[] primitiveArray = (double[]) value;
            Object[] boxedArray = new Double[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof char[]) {
            char[] primitiveArray = (char[]) value;
            Object[] boxedArray = new Character[primitiveArray.length];
            for (int i = 0; i < primitiveArray.length; i++) {
                boxedArray[i] = primitiveArray[i];
            }
            return boxedArray;
        } else if (value instanceof Object[]) {
            return (Object[]) value;
        } else {
            throw new IllegalArgumentException("Not an array: " + value);
        }
    }
}
