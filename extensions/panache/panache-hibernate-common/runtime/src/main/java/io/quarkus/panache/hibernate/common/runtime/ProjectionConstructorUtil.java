package io.quarkus.panache.hibernate.common.runtime;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.panache.common.exception.PanacheQueryException;

/**
 * Shared support for Panache DTO projection constructor selection and parameter mapping.
 * <p>
 * Kotlin data classes (especially with {@code @JvmInline} value classes and default parameters)
 * generate synthetic constructors whose extra parameters have no names. This utility skips those
 * constructors and maps only the real component parameters to entity fields.
 */
public final class ProjectionConstructorUtil {

    private static final Set<String> PROJECTED_CONSTRUCTOR_ANNOTATIONS = Set.of(
            "io.quarkus.hibernate.orm.panache.common.ProjectedConstructor",
            "io.quarkus.hibernate.reactive.panache.common.ProjectedConstructor");

    private static final Set<String> PROJECTED_FIELD_NAME_ANNOTATIONS = Set.of(
            "io.quarkus.hibernate.orm.panache.common.ProjectedFieldName",
            "io.quarkus.hibernate.reactive.panache.common.ProjectedFieldName");

    private static final Set<String> NESTED_PROJECTED_CLASS_ANNOTATIONS = Set.of(
            "io.quarkus.hibernate.orm.panache.common.NestedProjectedClass",
            "io.quarkus.hibernate.reactive.panache.common.NestedProjectedClass");

    private static final String JVM_INLINE_ANNOTATION = "kotlin.jvm.JvmInline";

    private ProjectionConstructorUtil() {
    }

    public static String buildSelectClause(Class<?> type,
            BiFunction<Class<?>, String, String> nestedProjectionBuilder) {
        return "SELECT " + buildConstructorExpression(type, null, nestedProjectionBuilder);
    }

    public static String buildConstructorExpression(Class<?> type, String parentParameter,
            BiFunction<Class<?>, String, String> nestedProjectionBuilder) {
        Constructor<?> constructor = getProjectionConstructor(type);
        String parametersListStr = getProjectionParameters(constructor).stream()
                .map(parameter -> getProjectionParameterName(type, parentParameter, parameter, nestedProjectionBuilder))
                .collect(Collectors.joining(","));
        return "new " + type.getName() + " (" + parametersListStr + ") ";
    }

    public static Constructor<?> getProjectionConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            if (hasAnnotation(constructor, PROJECTED_CONSTRUCTOR_ANNOTATIONS)) {
                return constructor;
            }
        }

        for (Constructor<?> constructor : constructors) {
            for (Parameter parameter : constructor.getParameters()) {
                if (hasAnnotation(parameter, PROJECTED_FIELD_NAME_ANNOTATIONS)) {
                    return constructor;
                }
            }
        }

        List<Constructor<?>> usableConstructors = new ArrayList<>();
        for (Constructor<?> constructor : constructors) {
            if (isUsableProjectionConstructor(constructor)) {
                usableConstructors.add(constructor);
            }
        }

        if (!usableConstructors.isEmpty()) {
            Constructor<?> selectedConstructor = null;
            int minParameterCount = Integer.MAX_VALUE;
            for (Constructor<?> constructor : usableConstructors) {
                int parameterCount = getProjectionParameters(constructor).size();
                if (parameterCount < minParameterCount) {
                    minParameterCount = parameterCount;
                    selectedConstructor = constructor;
                }
            }
            return selectedConstructor;
        }

        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > 0) {
                throw new PanacheQueryException(buildNoSuitableConstructorMessage(type, constructor));
            }
        }
        throw new PanacheQueryException("No suitable projection constructor found for " + type.getName()
                + ". Projection DTOs require a constructor with at least one parameter.");
    }

    public static String getProjectionParameterName(Class<?> parentType, String parentParameter, Parameter parameter,
            BiFunction<Class<?>, String, String> nestedProjectionBuilder) {
        String parameterName;
        if (hasProjectedFieldName(parameter)) {
            parameterName = getNameFromProjectedFieldName(parameter);
        } else if (!parameter.isNamePresent()) {
            throw new PanacheQueryException(
                    "Your application must be built with parameter names, this should be the default if"
                            + " using Quarkus project generation. Check the Maven or Gradle compiler configuration to include '-parameters'."
                            + " When using Kotlin data classes with value classes or default parameters, Panache skips synthetic"
                            + " constructors automatically; if this error persists, annotate the constructor with @ProjectedConstructor"
                            + " or annotate parameters with @ProjectedFieldName.");
        } else {
            try {
                Field field = parentType.getDeclaredField(parameter.getName());
                parameterName = hasProjectedFieldName(field) ? getNameFromProjectedFieldName(field)
                        : parameter.getName();
            } catch (NoSuchFieldException e) {
                parameterName = parameter.getName();
            }
        }
        parameterName = parentParameter == null ? parameterName : parentParameter + "." + parameterName;
        if (hasNestedProjectedClass(parameter.getType())) {
            return nestedProjectionBuilder.apply(parameter.getType(), parameterName);
        }
        return wrapInlineValueClass(parameter.getType(), parameterName);
    }

    private static String wrapInlineValueClass(Class<?> parameterType, String entityPath) {
        if (!isInlineValueClass(parameterType)) {
            return entityPath;
        }
        return "new " + parameterType.getName() + "(" + entityPath + ")";
    }

    private static boolean isInlineValueClass(Class<?> type) {
        if (type == null || type.isPrimitive()) {
            return false;
        }
        if (!hasAnnotation(type, JVM_INLINE_ANNOTATION)) {
            return false;
        }
        try {
            Field valueField = type.getDeclaredField("value");
            return !Modifier.isStatic(valueField.getModifiers());
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static List<Parameter> getProjectionParameters(Constructor<?> constructor) {
        return Stream.of(constructor.getParameters())
                .filter(parameter -> !isKotlinSyntheticParameter(parameter))
                .collect(Collectors.toList());
    }

    private static boolean isUsableProjectionConstructor(Constructor<?> constructor) {
        if (constructor.isSynthetic()) {
            return false;
        }
        List<Parameter> parameters = getProjectionParameters(constructor);
        if (parameters.isEmpty()) {
            return false;
        }
        for (Parameter parameter : parameters) {
            if (!parameter.isNamePresent()) {
                return false;
            }
        }
        return true;
    }

    static boolean isKotlinSyntheticParameter(Parameter parameter) {
        if (hasProjectedFieldName(parameter)) {
            return false;
        }
        String typeName = parameter.getType().getName();
        if (typeName.startsWith("kotlin.jvm.internal.")) {
            return true;
        }
        return parameter.getType() == int.class && !parameter.isNamePresent();
    }

    private static boolean hasProjectedFieldName(AnnotatedElement annotatedElement) {
        return hasAnnotation(annotatedElement, PROJECTED_FIELD_NAME_ANNOTATIONS);
    }

    private static boolean hasNestedProjectedClass(Class<?> type) {
        return hasAnnotation(type, NESTED_PROJECTED_CLASS_ANNOTATIONS);
    }

    private static boolean hasAnnotation(AnnotatedElement annotatedElement, Set<String> annotationTypeNames) {
        for (java.lang.annotation.Annotation annotation : annotatedElement.getAnnotations()) {
            if (annotationTypeNames.contains(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotation(AnnotatedElement annotatedElement, String annotationTypeName) {
        for (java.lang.annotation.Annotation annotation : annotatedElement.getAnnotations()) {
            if (annotationTypeName.equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private static String getNameFromProjectedFieldName(AnnotatedElement annotatedElement) {
        for (java.lang.annotation.Annotation annotation : annotatedElement.getAnnotations()) {
            if (PROJECTED_FIELD_NAME_ANNOTATIONS.contains(annotation.annotationType().getName())) {
                try {
                    String name = (String) annotation.annotationType().getMethod("value").invoke(annotation);
                    if (name.isEmpty()) {
                        throw new PanacheQueryException("The annotation ProjectedFieldName must have a non-empty value.");
                    }
                    return name;
                } catch (ReflectiveOperationException e) {
                    throw new PanacheQueryException("Unable to read ProjectedFieldName value", e);
                }
            }
        }
        throw new PanacheQueryException("Missing ProjectedFieldName annotation");
    }

    private static String buildNoSuitableConstructorMessage(Class<?> type, Constructor<?> rejected) {
        StringBuilder message = new StringBuilder("No suitable projection constructor found for ")
                .append(type.getName())
                .append(" (rejected constructor: ")
                .append(rejected)
                .append(").");
        if (isKotlinClass(type)) {
            message.append(" Kotlin value classes and default parameters may produce synthetic constructors.")
                    .append(" Use @ProjectedConstructor, @ProjectedFieldName, or a DTO with plain property types such as Long.");
        } else {
            message.append(" Use @ProjectedConstructor or @ProjectedFieldName to select a usable constructor,")
                    .append(" and ensure the application is built with parameter names (-parameters).");
        }
        return message.toString();
    }

    private static boolean isKotlinClass(Class<?> type) {
        for (java.lang.annotation.Annotation annotation : type.getAnnotations()) {
            if ("kotlin.Metadata".equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }
}
