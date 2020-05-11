package io.quarkus.runtime.configuration;

import static io.quarkus.annotation.processor.Constants.EMPTY;
import static io.quarkus.annotation.processor.Constants.QUARKUS;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.smallrye.config.Converters;
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

    // certain well-known classname suffixes that we support
    private static Set<String> supportedClassNameSuffix;

    static {
        final Set<String> suffixes = new HashSet<>();
        suffixes.add("Config");
        suffixes.add("Configuration");
        supportedClassNameSuffix = Collections.unmodifiableSet(suffixes);
    }

    public static void handleObject(Object o) {
        final SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        final Class cls = o.getClass();
        final String clsNameSuffix = getClassNameSuffix(o);
        if (clsNameSuffix == null) {
            // unsupported object type
            return;
        }
        final String name = dashify(cls.getSimpleName().substring(0, cls.getSimpleName().length() - clsNameSuffix.length()));
        handleObject(QUARKUS + "." + name, o, config);
    }

    private static void handleObject(String prefix, Object o, SmallRyeConfig config) {

        try {
            final Class cls = o.getClass();
            if (!isClassNameSuffixSupported(o)) {
                return;
            }
            for (Field field : cls.getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                ConfigItem configItem = field.getDeclaredAnnotation(ConfigItem.class);
                final Class<?> fieldClass = field.getType();
                if (configItem == null || fieldClass.isAnnotationPresent(ConfigGroup.class)) {
                    Constructor<?> constructor = fieldClass.getConstructor();
                    constructor.setAccessible(true);
                    Object newInstance = constructor.newInstance();
                    field.set(o, newInstance);
                    handleObject(prefix + "." + dashify(field.getName()), newInstance, config);
                } else if (fieldClass == Map.class) { //TODO: FIXME, this cannot handle Map yet
                    field.set(o, new HashMap<>());
                } else {
                    String name = configItem.name();
                    if (name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                        name = dashify(field.getName());
                    } else if (name.equals(ConfigItem.ELEMENT_NAME)) {
                        name = field.getName();
                    }

                    final Type genericType = field.getGenericType();
                    final Converter<?> conv = getConverterFor(genericType);

                    // Check if the value exists for name
                    final String fullName = prefix + "." + name;
                    boolean success = setValueIfPresent(o, field, fullName, config, conv);
                    if (success) {
                        continue;
                    }

                    // Check if the value exists for deprecated names
                    String[] deprecatedNames = configItem.deprecatedNames();
                    for (String deprecatedName : deprecatedNames) {
                        if (deprecatedName == null || EMPTY.equals(deprecatedName)) {
                            continue;
                        }
                        String deprecatedFullName = prefix + "." + deprecatedName;
                        success = setValueIfPresent(o, field, deprecatedFullName, config, conv);
                        if (success) {
                            // Only first one to get resolved will be used, ignore following names
                            ConfigDiagnostic.deprecated(deprecatedFullName, fullName);
                            break;
                        }
                    }

                    // Checks for the default value
                    if (!success && !configItem.defaultValue().equals(ConfigItem.NO_DEFAULT)) {
                        //the runtime config source handles default automatically
                        //however this may not have actually been installed depending on where the failure occurred
                        field.set(o, conv.convert(configItem.defaultValue()));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean setValueIfPresent(Object obj, Field field, String name, SmallRyeConfig config,
            Converter<?> converter) throws IllegalAccessException {
        try {
            Optional<?> value = config.getOptionalValue(name, converter);
            if (value.isPresent()) {
                field.set(obj, value.get());
                return true;
            }
        } catch (NoSuchElementException ignored) {
        }

        return false;
    }

    private static Converter<?> getConverterFor(Type type) {
        // hopefully this is enough
        final SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        Class<?> rawType = rawTypeOf(type);
        if (Enum.class.isAssignableFrom(rawType)) {
            return new HyphenateEnumConverter(rawType);
        } else if (rawType == Optional.class) {
            return Converters.newOptionalConverter(getConverterFor(typeOfParameterFirstValue(type)));
        } else if (rawType == List.class) {
            return Converters.newCollectionConverter(getConverterFor(typeOfParameterFirstValue(type)), ArrayList::new);
        } else {
            return config.getConverter(rawTypeOf(type));
        }
    }

    // cribbed from io.quarkus.deployment.util.ReflectUtil
    private static Class<?> rawTypeOf(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return rawTypeOf(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            return Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        } else {
            throw new IllegalArgumentException("Type has no raw type class: " + type);
        }
    }

    private static Type typeOfParameterFirstValue(final Type type) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException("Type is not parameterized: " + type);
        }
    }

    //    Configuration keys are normally derived from the field names that they are tied to.
    //    This is done by de-camel-casing the name and then joining the segments with hyphens (-).
    //    Some examples:
    //    bindAddress becomes bind-address
    //    keepAliveTime becomes keep-alive-time
    //    requestDNSTimeout becomes request-dns-timeout
    private static String dashify(String substring) {
        final StringBuilder ret = new StringBuilder();
        final char[] chars = substring.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (i != 0 && i != (chars.length - 1) && c >= 'A' && c <= 'Z') {
                ret.append('-');
            }
            ret.append(Character.toLowerCase(c));
        }
        return ret.toString();
    }

    private static String getClassNameSuffix(final Object o) {
        if (o == null) {
            return null;
        }
        final String klassName = o.getClass().getName();
        for (final String supportedSuffix : supportedClassNameSuffix) {
            if (klassName.endsWith(supportedSuffix)) {
                return supportedSuffix;
            }
        }
        return null;
    }

    private static boolean isClassNameSuffixSupported(final Object o) {
        if (o == null) {
            return false;
        }
        final String klassName = o.getClass().getName();
        for (final String supportedSuffix : supportedClassNameSuffix) {
            if (klassName.endsWith(supportedSuffix)) {
                return true;
            }
        }
        return false;
    }
}
