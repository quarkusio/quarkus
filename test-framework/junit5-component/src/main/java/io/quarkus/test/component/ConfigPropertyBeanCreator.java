package io.quarkus.test.component;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;

import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.config.inject.ConfigProducerUtil;

public class ConfigPropertyBeanCreator implements BeanCreator<Object> {

    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        InjectionPoint injectionPoint = context.getInjectedReference(InjectionPoint.class);
        if (Boolean.TRUE.equals(context.getParams().get("useDefaultConfigProperties"))) {
            try {
                return ConfigProducerUtil.getValue(injectionPoint, ConfigBeanCreator.getConfig());
            } catch (NoSuchElementException e) {
                Class<?> rawType = getRawType(injectionPoint.getType());
                if (rawType == null) {
                    throw new IllegalStateException("Unable to get the raw type for: " + injectionPoint.getType());
                }
                if (rawType.isPrimitive()) {
                    if (rawType == boolean.class) {
                        return false;
                    } else if (rawType == char.class) {
                        return Character.MIN_VALUE;
                    } else {
                        return 0;
                    }
                }
                return null;
            }
        } else {
            return ConfigProducerUtil.getValue(injectionPoint, ConfigBeanCreator.getConfig());
        }

    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        if (type instanceof ParameterizedType) {
            if (((ParameterizedType) type).getRawType() instanceof Class<?>) {
                return (Class<T>) ((ParameterizedType) type).getRawType();
            }
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Class<?> rawType = getRawType(genericArrayType.getGenericComponentType());
            if (rawType != null) {
                return (Class<T>) Array.newInstance(rawType, 0).getClass();
            }
        }
        return null;
    }

}
