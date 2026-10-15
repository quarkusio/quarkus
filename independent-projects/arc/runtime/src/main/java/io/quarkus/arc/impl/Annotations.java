package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class Annotations {
    /**
     * Returns whether the given {@code requiredAnnotations} are all present in the given set of {@code annotations},
     * following the CDI rules. That is, {@code @Nonbinding} annotation members are assumed to be equal without
     * actually looking at their values. Which annotation members are nonbinding is obtained from the given
     * map of {@code nonbindingMembers}, where the key is the annotation class name and the value is the set of
     * names of nonbinding members.
     */
    static boolean areAllPresent(Set<Annotation> requiredAnnotations, Iterable<Annotation> annotations,
            Map<String, Set<String>> nonbindingMembers) {
        for (Annotation required : requiredAnnotations) {
            if (!isPresent(required, annotations, nonbindingMembers)) {
                return false;
            }
        }
        return true;

    }

    /**
     * Returns whether the given {@code requiredAnnotation} is present in the given set of {@code annotations},
     * following the CDI rules. That is, {@code @Nonbinding} annotation members are assumed to be equal without
     * actually looking at their values. Which annotation members are nonbinding is obtained from the given
     * map of {@code nonbindingMembers}, where the key is the annotation class name and the value is the set of
     * names of nonbinding members.
     */
    static boolean isPresent(Annotation requiredAnnotation, Iterable<Annotation> annotations,
            Map<String, Set<String>> nonbindingMembers) {
        Class<? extends Annotation> requiredClass = requiredAnnotation.annotationType();
        Method[] members = requiredClass.getDeclaredMethods();

        Set<String> nonbinding = nonbindingMembers.get(requiredClass.getName());

        for (Annotation annotation : annotations) {
            Class<? extends Annotation> clazz = annotation.annotationType();
            if (!clazz.equals(requiredClass)) {
                continue;
            }
            boolean matches = true;
            for (Method value : members) {
                if (nonbinding != null && nonbinding.contains(value.getName())) {
                    continue;
                }

                Object val1 = invoke(value, requiredAnnotation);
                Object val2 = invoke(value, annotation);
                if (val1.getClass().isArray()) {
                    if (!val2.getClass().isArray() || !arraysEqual(val1, val2)) {
                        matches = false;
                        break;
                    }
                } else if (!val1.equals(val2)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static Object invoke(Method method, Object instance) {
        try {
            method.setAccessible(true);
            return method.invoke(instance);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(
                    "Error checking value of member method " + method.getName() + " on " + method.getDeclaringClass(), e);
        }
    }

    // qualifiers cannot have array-valued (or annotation-valued) members that are not `@Nonbinding`,
    // see also `BeanDeployment.validateQualifier()`, but no such restriction applies to interceptor bindings
    private static boolean arraysEqual(Object val1, Object val2) {
        if (val1 instanceof Object[] arr1 && val2 instanceof Object[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof boolean[] arr1 && val2 instanceof boolean[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof byte[] arr1 && val2 instanceof byte[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof short[] arr1 && val2 instanceof short[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof int[] arr1 && val2 instanceof int[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof long[] arr1 && val2 instanceof long[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof float[] arr1 && val2 instanceof float[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof double[] arr1 && val2 instanceof double[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else if (val1 instanceof char[] arr1 && val2 instanceof char[] arr2) {
            return Arrays.equals(arr1, arr2);
        } else {
            return Objects.equals(val1, val2);
        }
    }
}
