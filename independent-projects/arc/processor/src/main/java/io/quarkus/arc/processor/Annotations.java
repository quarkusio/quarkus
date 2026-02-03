package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

public final class Annotations {

    private Annotations() {
    }

    /**
     *
     * @param annotations
     * @param name
     * @return the first matching annotation instance with the given name or null
     */
    public static AnnotationInstance find(Collection<AnnotationInstance> annotations, DotName name) {
        if (annotations.isEmpty()) {
            return null;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            if (annotationInstance.name().equals(name)) {
                return annotationInstance;
            }
        }
        return null;
    }

    /**
     *
     * @param annotations
     * @param name
     * @return {@code true} if the given collection contains an annotation instance with the given name, {@code false} otherwise
     */
    public static boolean contains(Collection<AnnotationInstance> annotations, DotName name) {
        return find(annotations, name) != null;
    }

    /**
     *
     * @param annotations
     * @param names
     * @return {@code true} if the given collection contains an annotation instance with any of the given names, {@code false}
     *         otherwise
     */
    public static boolean containsAny(Collection<AnnotationInstance> annotations, Iterable<DotName> names) {
        if (annotations.isEmpty()) {
            return false;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            for (DotName name : names) {
                if (annotationInstance.name().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param annotations
     * @return the parameter annotations
     */
    public static Set<AnnotationInstance> getParameterAnnotations(Collection<AnnotationInstance> annotations) {
        return getAnnotations(Kind.METHOD_PARAMETER, annotations);
    }

    /**
     *
     * @param annotations
     * @return the annotations for the given kind
     */
    public static Set<AnnotationInstance> getAnnotations(Kind kind, Collection<AnnotationInstance> annotations) {
        return getAnnotations(kind, null, annotations);
    }

    /**
     *
     * @param annotations
     * @return the annotations for the given kind and name
     */
    public static Set<AnnotationInstance> getAnnotations(Kind kind, DotName name, Collection<AnnotationInstance> annotations) {
        if (annotations.isEmpty()) {
            return Collections.emptySet();
        }
        Set<AnnotationInstance> ret = new HashSet<>();
        for (AnnotationInstance annotation : annotations) {
            if (kind != annotation.target().kind()) {
                continue;
            }
            if (name != null && !annotation.name().equals(name)) {
                continue;
            }
            ret.add(annotation);
        }
        return ret;
    }

    /**
     *
     * @param beanDeployment
     * @param method
     * @param name
     * @return whether given method has a parameter that has an annotation with given name
     */
    public static boolean hasParameterAnnotation(BeanDeployment beanDeployment, MethodInfo method, DotName name) {
        return contains(getParameterAnnotations(beanDeployment, method), name);
    }

    /**
     *
     * @param beanDeployment
     * @param method
     * @return collection of annotations present on all parameters of given method
     */
    public static Set<AnnotationInstance> getParameterAnnotations(BeanDeployment beanDeployment, MethodInfo method) {
        Set<AnnotationInstance> annotations = new HashSet<>();
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(method)) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind()) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    /**
     *
     * @param beanDeployment
     * @param method
     * @param position
     * @return the parameter annotations for the given position
     */
    public static Set<AnnotationInstance> getParameterAnnotations(BeanDeployment beanDeployment, MethodInfo method,
            int position) {
        return getParameterAnnotations(beanDeployment::getAnnotations, method, position);
    }

    /**
     *
     * @param transformedAnnotations
     * @param method
     * @param position
     * @return the parameter annotations for the given position
     */
    public static Set<AnnotationInstance> getParameterAnnotations(
            Function<AnnotationTarget, Collection<AnnotationInstance>> transformedAnnotations, MethodInfo method,
            int position) {
        Set<AnnotationInstance> annotations = new HashSet<>();
        for (AnnotationInstance annotation : transformedAnnotations.apply(method)) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind()
                    && annotation.target().asMethodParameter().position() == position) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    /**
     * Iterates over all annotations on a method and its parameters, filters out all non-parameter annotations
     * and returns a first encountered {@link AnnotationInstance} with Annotation specified as {@link DotName}.
     * Returns {@code null} if no such annotation exists.
     *
     * @param method MethodInfo to be searched for annotations
     * @param annotation Annotation we are looking for, represented as DotName
     * @return First encountered {@link AnnotationInstance} fitting the requirements, {@code null} if none is found
     */
    public static AnnotationInstance getParameterAnnotation(MethodInfo method, DotName annotation) {
        for (AnnotationInstance annotationInstance : method.annotations()) {
            if (annotationInstance.target().kind().equals(Kind.METHOD_PARAMETER) &&
                    annotationInstance.name().equals(annotation)) {
                return annotationInstance;
            }
        }
        return null;
    }

    public static Collection<AnnotationInstance> onlyRuntimeVisible(Collection<AnnotationInstance> annotations) {
        List<AnnotationInstance> result = new ArrayList<>(annotations.size());
        for (AnnotationInstance annotation : annotations) {
            if (annotation.runtimeVisible()) {
                result.add(annotation);
            }
        }
        return result;
    }

    /**
     * Returns a list of annotations created out of the given set such that the result does not have
     * annotations that are identical except of the {@linkplain AnnotationInstance#target() annotation target}.
     */
    public static List<AnnotationInstance> uniqueAnnotations(Set<AnnotationInstance> annotations) {
        Set<AnnotationInstanceEquivalenceProxy> proxies = new HashSet<>();
        for (AnnotationInstance annotation : annotations) {
            proxies.add(annotation.createEquivalenceProxy());
        }
        List<AnnotationInstance> result = new ArrayList<>(proxies.size());
        for (AnnotationInstanceEquivalenceProxy proxy : proxies) {
            result.add(proxy.get());
        }
        return result;
    }

    public static org.jboss.jandex.AnnotationInstance jandexAnnotation(Annotation annotation) {
        Class<? extends Annotation> annotationType = annotationType(annotation);

        DotName name = DotName.createSimple(annotationType.getName());
        @SuppressWarnings("unchecked")
        org.jboss.jandex.AnnotationValue[] jandexAnnotationValues = jandexAnnotationValues(
                (Class<Annotation>) annotationType, annotation);

        return org.jboss.jandex.AnnotationInstance.create(name, null, jandexAnnotationValues);
    }

    @SuppressWarnings("unchecked")
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
            return org.jboss.jandex.AnnotationValue.createClassValue(name, Types.jandexType((Class<?>) value));
        } else if (value.getClass().isAnnotation()) {
            Class<? extends Annotation> annotationType = annotationType((Annotation) value);
            @SuppressWarnings("unchecked")
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
