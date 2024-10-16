package io.quarkus.runtime.configuration;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.Converters;
import io.smallrye.config.SmallRyeConfig;

/**
 * Utility class for manually instantiating a config object
 * <p>
 * This should only be used in specific circumstances, generally when normal start
 * has failed and we are attempting to do some form of recovery via hot deployment
 * <p>
 * TODO: fully implement this as required, at the moment this is mostly to read the HTTP config when startup fails
 * or for basic logging setup in non-Quarkus tests
 */
public class ConfigInstantiator {

    // certain well-known classname suffixes that we support
    private static Set<String> SUPPORTED_CLASS_NAME_SUFFIXES = Set.of("Config", "Configuration");

    private static final String QUARKUS_PROPERTY_PREFIX = "quarkus.";

    private static final Pattern SEGMENT_EXTRACTION_PATTERN = Pattern.compile("(\"[^\"]+\"|[^.\"]+).*");

    public static <T> T handleObject(Supplier<T> supplier) {
        T o = supplier.get();
        handleObject(o);
        return o;
    }

    public static void handleObject(Object o) {
        final SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        handleObject(o, config);
    }

    public static void handleObject(Object o, SmallRyeConfig config) {
        final String clsNameSuffix = getClassNameSuffix(o);
        if (clsNameSuffix == null) {
            // unsupported object type
            return;
        }

        final Class<?> cls = o.getClass();
        final String name;
        ConfigRoot configRoot = cls.getAnnotation(ConfigRoot.class);
        if (configRoot != null && !configRoot.name().equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
            name = configRoot.name();
            if (name.startsWith("<<")) {
                throw new IllegalArgumentException("Found unsupported @ConfigRoot.name = " + name + " on " + cls);
            }
        } else {
            name = dashify(cls.getSimpleName().substring(0, cls.getSimpleName().length() - clsNameSuffix.length()));
        }
        handleObject(QUARKUS_PROPERTY_PREFIX + name, o, config, gatherQuarkusPropertyNames(config));
    }

    private static List<String> gatherQuarkusPropertyNames(SmallRyeConfig config) {
        var names = new ArrayList<String>(50);
        for (String name : config.getPropertyNames()) {
            if (name.startsWith(QUARKUS_PROPERTY_PREFIX)) {
                names.add(name);
            }
        }
        return names;
    }

    private static void handleObject(String prefix, Object o, SmallRyeConfig config, List<String> quarkusPropertyNames) {

        try {
            final Class<?> cls = o.getClass();
            if (!isClassNameSuffixSupported(o)) {
                return;
            }
            for (Field field : cls.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isFinal(field.getModifiers())) {
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
                    handleObject(prefix + "." + dashify(field.getName()), newInstance, config, quarkusPropertyNames);
                } else {
                    String name = configItem.name();
                    if (name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                        name = dashify(field.getName());
                    } else if (name.equals(ConfigItem.ELEMENT_NAME)) {
                        name = field.getName();
                    }
                    String fullName = prefix + "." + name;
                    final Type genericType = field.getGenericType();
                    if (fieldClass == Map.class) {
                        field.set(o, handleMap(fullName, genericType, config, quarkusPropertyNames));
                        continue;
                    }
                    final Converter<?> conv = getConverterFor(genericType, config);
                    try {
                        Optional<?> value = config.getOptionalValue(fullName, conv);
                        if (value.isPresent()) {
                            field.set(o, value.get());
                        } else if (!configItem.defaultValue().equals(ConfigItem.NO_DEFAULT)) {
                            //the runtime config source handles default automatically
                            //however this may not have actually been installed depending on where the failure occurred
                            field.set(o, conv.convert(configItem.defaultValue()));
                        }
                    } catch (NoSuchElementException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<?, ?> handleMap(String fullName, Type genericType, SmallRyeConfig config,
            List<String> quarkusPropertyNames) throws ReflectiveOperationException {
        var map = new HashMap<>();
        if (typeOfParameter(genericType, 0) != String.class) { // only support String keys
            return map;
        }
        var processedSegments = new HashSet<String>();
        // infer the map keys from existing property names
        for (String propertyName : quarkusPropertyNames) {
            var fullNameWithDot = fullName + ".";
            String withoutPrefix = propertyName.replace(fullNameWithDot, "");
            if (withoutPrefix.equals(propertyName)) {
                continue;
            }
            Matcher matcher = SEGMENT_EXTRACTION_PATTERN.matcher(withoutPrefix);
            if (!matcher.find()) {
                continue; // should not happen, but be lenient
            }
            var segment = matcher.group(1);
            if (!processedSegments.add(segment)) {
                continue;
            }
            var mapKey = segment.replace("\"", "");
            var nextFullName = fullNameWithDot + segment;
            var mapValueType = typeOfParameter(genericType, 1);
            Object mapValue;
            if (mapValueType instanceof ParameterizedType
                    && ((ParameterizedType) mapValueType).getRawType().equals(Map.class)) {
                mapValue = handleMap(nextFullName, mapValueType, config, quarkusPropertyNames);
            } else {
                Class<?> mapValueClass = mapValueType instanceof Class ? (Class<?>) mapValueType : null;
                if (mapValueClass != null && mapValueClass.isAnnotationPresent(ConfigGroup.class)) {
                    Constructor<?> constructor = mapValueClass.getConstructor();
                    constructor.setAccessible(true);
                    mapValue = constructor.newInstance();
                    handleObject(nextFullName, mapValue, config, quarkusPropertyNames);
                } else {
                    final Converter<?> conv = getConverterFor(mapValueType, config);
                    mapValue = config.getOptionalValue(nextFullName, conv).orElse(null);
                }
            }
            map.put(mapKey, mapValue);
        }
        return map;
    }

    private static Converter<?> getConverterFor(Type type, SmallRyeConfig config) {
        // hopefully this is enough
        Class<?> rawType = rawTypeOf(type);
        if (Enum.class.isAssignableFrom(rawType)) {
            return Converters.getImplicitConverter(rawType);
        } else if (rawType == Optional.class) {
            return Converters.newOptionalConverter(getConverterFor(typeOfParameter(type, 0), config));
        } else if (rawType == List.class) {
            return Converters.newCollectionConverter(getConverterFor(typeOfParameter(type, 0), config),
                    ConfigUtils.listFactory());
        } else if (rawType == Set.class) {
            return Converters.newCollectionConverter(getConverterFor(typeOfParameter(type, 0), config),
                    ConfigUtils.setFactory());
        } else {
            return config.requireConverter(rawTypeOf(type));
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

    static Type typeOfParameter(final Type type, final int paramIdx) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments()[paramIdx];
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
        for (final String supportedSuffix : SUPPORTED_CLASS_NAME_SUFFIXES) {
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
        for (final String supportedSuffix : SUPPORTED_CLASS_NAME_SUFFIXES) {
            if (klassName.endsWith(supportedSuffix)) {
                return true;
            }
        }
        return false;
    }
}
