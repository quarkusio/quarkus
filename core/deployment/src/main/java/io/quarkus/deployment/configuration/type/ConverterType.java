package io.quarkus.deployment.configuration.type;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.annotations.DefaultConverter;
import io.quarkus.runtime.configuration.HyphenateEnumConverter;

/**
 *
 */
public abstract class ConverterType {
    ConverterType() {
    }

    public abstract Class<?> getLeafType();

    public static ConverterType of(Field member) {
        return of(member.getGenericType(), member);
    }

    public static ConverterType of(Parameter parameter) {
        return of(parameter.getParameterizedType(), parameter);
    }

    public static ConverterType of(Type type, AnnotatedElement element) {
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return new ArrayOf(of(genericArrayType.getGenericComponentType(), element));
        } else if (type instanceof Class<?>) {
            // simple type
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return new ArrayOf(of(clazz.getComponentType(), element));
            }
            ConvertWith convertWith = element.getAnnotation(ConvertWith.class);
            Leaf leaf;
            if (convertWith == null && element.getAnnotation(DefaultConverter.class) == null && clazz.isEnum()) {
                // use our hyphenated converter by default
                leaf = new Leaf(clazz, HyphenateEnumConverter.class);
            } else {
                leaf = new Leaf(clazz, convertWith == null ? null : convertWith.value());
            }
            // vvv todo: add validations here vvv
            // return result
            return leaf;
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType paramType = (ParameterizedType) type;
            final Class<?> rawType = ReflectUtil.rawTypeOf(paramType);
            final Type[] args = paramType.getActualTypeArguments();
            if (args.length == 1) {
                final Type arg = args[0];
                if (rawType == Class.class) {
                    ConverterType result = of(Class.class, element);
                    if (arg instanceof WildcardType) {
                        final WildcardType wcType = (WildcardType) arg;
                        // gather bounds for validation
                        Class<?>[] upperBounds = ReflectUtil.rawTypesOfDestructive(wcType.getUpperBounds());
                        Class<?>[] lowerBounds = ReflectUtil.rawTypesOfDestructive(wcType.getLowerBounds());
                        for (Class<?> upperBound : upperBounds) {
                            if (upperBound != Object.class) {
                                result = new UpperBoundCheckOf(upperBound, result);
                            }
                        }
                        for (Class<?> lowerBound : lowerBounds) {
                            result = new LowerBoundCheckOf(lowerBound, result);
                        }
                        return result;
                    }
                    throw new IllegalArgumentException("Class configuration item types cannot be invariant");
                }
                final ConverterType nested = of(arg, element);
                if (rawType == List.class || rawType == Set.class || rawType == SortedSet.class
                        || rawType == NavigableSet.class) {
                    return new CollectionOf(nested, rawType);
                } else if (rawType == Optional.class) {
                    return new OptionalOf(nested);
                } else {
                    throw unsupportedType(type);
                }
            } else if (args.length == 2) {
                if (rawType == Map.class) {
                    // the real converter is the converter for the value type
                    return of(ReflectUtil.typeOfParameter(paramType, 1), element);
                } else {
                    throw unsupportedType(type);
                }
            } else {
                throw unsupportedType(type);
            }
        } else {
            throw unsupportedType(type);
        }
    }

    private static IllegalArgumentException unsupportedType(final Type type) {
        return new IllegalArgumentException("Unsupported type: " + type);
    }
}
