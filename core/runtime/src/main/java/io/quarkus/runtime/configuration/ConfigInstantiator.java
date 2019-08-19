package io.quarkus.runtime.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
                if (configItem == null || field.getType().isAnnotationPresent(ConfigGroup.class)) {
                    Object newInstance = field.getType().newInstance();
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
                    Optional<?> val = config.getOptionalValue(fullName, field.getType());
                    if (val.isPresent()) {
                        field.set(o, val.get());
                    } else if (defaultValue != null) {
                        if (field.getType().equals(List.class)) {
                            Class<?> listType = (Class<?>) ((ParameterizedType) field.getGenericType())
                                    .getActualTypeArguments()[0];
                            String[] parts = defaultValue.split(",");
                            List<Object> list = new ArrayList<>();
                            for (String i : parts) {
                                list.add(config.convert(i, listType));
                            }
                            field.set(o, list);
                        } else if (field.getType().equals(Optional.class)) {
                            Class<?> optionalType = (Class<?>) ((ParameterizedType) field.getGenericType())
                                    .getActualTypeArguments()[0];
                            field.set(o, Optional.of(config.convert(defaultValue, optionalType)));
                        } else {
                            field.set(o, config.convert(defaultValue, field.getType()));
                        }
                    } else if (field.getType().equals(Optional.class)) {
                        field.set(o, Optional.empty());
                    } else if (field.getType().equals(OptionalInt.class)) {
                        field.set(o, OptionalInt.empty());
                    } else if (field.getType().equals(OptionalDouble.class)) {
                        field.set(o, OptionalDouble.empty());
                    } else if (field.getType().equals(OptionalLong.class)) {
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
