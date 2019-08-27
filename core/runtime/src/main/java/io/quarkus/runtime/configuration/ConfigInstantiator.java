package io.quarkus.runtime.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.smallrye.config.SmallRyeConfig;

/**
 * Utility class for manually instantiating a config object
 * <p>
 * This should only be used in specific circumstances, generally when normal start
 * has failed and we are attempting to do some form of recovery via hot deployment
 * <p>
 * TODO: fully implement this as required, at the moment this is mostly to read the HTTP config when startup fails
 */
public class ConfigInstantiator {

    public static void handleObject(Object o) {
        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        Class cls = o.getClass();
        String name = dashify(cls.getSimpleName().substring(0, cls.getSimpleName().length() - "Config".length()));
        handleObject("quarkus." + name, o, config);
    }

    private static void handleObject(String prefix, Object o, SmallRyeConfig config) {

        try {
            Class cls = o.getClass();
            if (!cls.getName().endsWith("Config") && !cls.getName().endsWith("Configuration")) {
                return;
            }
            for (Field field : cls.getDeclaredFields()) {
                ConfigItem configItem = field.getDeclaredAnnotation(ConfigItem.class);
                final Class<?> fieldClass = field.getType();
                if (configItem == null || fieldClass.isAnnotationPresent(ConfigGroup.class)) {
                    Object newInstance = fieldClass.getConstructor().newInstance();
                    field.set(o, newInstance);
                    handleObject(prefix + "." + dashify(field.getName()), newInstance, config);
                } else {
                    String name = configItem.name();
                    if (name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                        name = dashify(field.getName());
                    }
                    String fullName = prefix + "." + name;
                    String defaultValue = configItem.defaultValue();
                    if (defaultValue.equals(ConfigItem.NO_DEFAULT)) {
                        defaultValue = null;
                    }
                    final Type genericType = field.getGenericType();
                    Optional<?> val;
                    final boolean fieldIsOptional = fieldClass.equals(Optional.class);
                    final boolean fieldIsList = fieldClass.equals(List.class);
                    if (fieldIsOptional) {
                        Class<?> actualType = (Class<?>) ((ParameterizedType) genericType)
                                .getActualTypeArguments()[0];
                        val = config.getOptionalValue(fullName, actualType);
                    } else if (fieldIsList) {
                        Class<?> actualType = (Class<?>) ((ParameterizedType) genericType)
                                .getActualTypeArguments()[0];
                        val = config.getOptionalValues(fullName, actualType, ArrayList::new);
                    } else {
                        val = config.getOptionalValue(fullName, fieldClass);
                    }
                    if (val.isPresent()) {
                        field.set(o, fieldIsOptional ? val : val.get());
                    } else if (defaultValue != null) {
                        if (fieldIsList) {
                            Class<?> listType = (Class<?>) ((ParameterizedType) genericType)
                                    .getActualTypeArguments()[0];
                            String[] parts = defaultValue.split(",");
                            List<Object> list = new ArrayList<>();
                            for (String i : parts) {
                                list.add(config.convert(i, listType));
                            }
                            field.set(o, list);
                        } else if (fieldIsOptional) {
                            Class<?> optionalType = (Class<?>) ((ParameterizedType) genericType)
                                    .getActualTypeArguments()[0];
                            field.set(o, Optional.of(config.convert(defaultValue, optionalType)));
                        } else {
                            field.set(o, config.convert(defaultValue, fieldClass));
                        }
                    } else if (fieldIsOptional) {
                        field.set(o, Optional.empty());
                    } else if (fieldClass.equals(OptionalInt.class)) {
                        field.set(o, OptionalInt.empty());
                    } else if (fieldClass.equals(OptionalDouble.class)) {
                        field.set(o, OptionalDouble.empty());
                    } else if (fieldClass.equals(OptionalLong.class)) {
                        field.set(o, OptionalLong.empty());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String dashify(String substring) {
        StringBuilder ret = new StringBuilder();
        for (char i : substring.toCharArray()) {
            if (i >= 'A' && i <= 'Z') {
                ret.append('-');
            }
            ret.append(Character.toLowerCase(i));
        }
        return ret.toString();
    }
}
