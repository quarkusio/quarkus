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
 * This should only be used in specific circumstances:
 * <ul>
 * <li>when normal start has failed and we are attempting to do some form of recovery via hot deployment</li>
 * <li>when processing config and needing to instantiate empty config groups to be used as fillers</li>
 * </ul>
 * <p>
 * TODO: fully implement this as required, at the moment this is mostly to read the HTTP config when startup fails
 * or for basic logging setup in non-Quarkus tests
 */
public class ConfigInstantiator {

    // certain well-known classname suffixes that we support
    private static final Set<String> SUPPORTED_CLASS_NAME_SUFFIXES = Set.of("Config", "Configuration");

    private static final String QUARKUS_PROPERTY_PREFIX = "quarkus.";

    private static final Pattern SEGMENT_EXTRACTION_PATTERN = Pattern.compile("(\"[^\"]+\"|[^.\"]+).*");

    public static <T> T handleObject(Supplier<T> supplier) {
        T o = supplier.get();
        handleObject(o);
        return o;
    }

    /**
     * @param clazz A config class (annotated with {@link ConfigRoot} or {@link ConfigGroup})
     * @return An empty instance of that class, with all fields initialized with their default value,
     *         as if the object had been instantiated from an empty configuration.
     * @param <T> The config type.
     */
    public static <T> T createEmptyObject(Class<T> clazz) {
        ConfigRoot configRoot = clazz.getAnnotation(ConfigRoot.class);
        ConfigGroup configGroup = clazz.getAnnotation(ConfigGroup.class);
        if (configRoot == null && configGroup == null) {
            throw new IllegalArgumentException("Class " + clazz + " is neither a config root nor a config group");
        }
        try {
            T object = clazz.getConstructor().newInstance();
            handleObject(QUARKUS_PROPERTY_PREFIX + "irrelevant-for-empty-object-creation", object,
                    InstantiationContext.withEmptyConfigSource());
            return object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleObject(Object o) {
        if (o == null) {
            // Nothing to do
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
            final String clsNameSuffix = getClassNameSuffix(o);
            if (clsNameSuffix == null) {
                // unsupported object type
                return;
            }
            name = dashify(cls.getSimpleName().substring(0, cls.getSimpleName().length() - clsNameSuffix.length()));
        }
        handleObject(QUARKUS_PROPERTY_PREFIX + name, o, InstantiationContext.withCurrentConfigSource());
    }

    private static void handleObject(String prefix, Object o, InstantiationContext context) {

        try {
            final Class<?> cls = o.getClass();
            if (!isSupportedRootOrGroupClass(cls)) {
                return;
            }
            for (Field field : cls.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                ConfigItem configItem = field.getDeclaredAnnotation(ConfigItem.class);
                final Class<?> fieldClass = field.getType();
                final Type genericType = field.getGenericType();
                String name = configItem == null ? null : configItem.name();
                if (name == null || name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                    name = dashify(field.getName());
                } else if (name.equals(ConfigItem.ELEMENT_NAME)) {
                    name = field.getName();
                } else if (name.equals(ConfigItem.PARENT)) {
                    name = null;
                }
                String fullName = prefix + (name == null ? "" : "." + name);
                if (fieldClass == Map.class) {
                    field.set(o, handleMap(fullName, genericType, context));
                } else if (configItem == null || fieldClass.isAnnotationPresent(ConfigGroup.class)) {
                    Constructor<?> constructor = fieldClass.getConstructor();
                    constructor.setAccessible(true);
                    Object newInstance = constructor.newInstance();
                    field.set(o, newInstance);
                    handleObject(fullName, newInstance, context);
                } else {
                    final Converter<?> conv = context.getConverterFor(genericType);
                    try {
                        Optional<?> value = context.getOptionalValue(fullName, conv);
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

    private static Map<?, ?> handleMap(String fullName, Type genericType, InstantiationContext context)
            throws ReflectiveOperationException {
        var map = new HashMap<>();
        if (typeOfParameter(genericType, 0) != String.class) { // only support String keys
            return map;
        }
        var processedSegments = new HashSet<String>();
        // infer the map keys from existing property names
        for (String propertyName : context.quarkusPropertyNames) {
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
                mapValue = handleMap(nextFullName, mapValueType, context);
            } else {
                Class<?> mapValueClass = mapValueType instanceof Class ? (Class<?>) mapValueType : null;
                if (mapValueClass != null && mapValueClass.isAnnotationPresent(ConfigGroup.class)) {
                    Constructor<?> constructor = mapValueClass.getConstructor();
                    constructor.setAccessible(true);
                    mapValue = constructor.newInstance();
                    handleObject(nextFullName, mapValue, context);
                } else {
                    final Converter<?> conv = context.getConverterFor(mapValueType);
                    mapValue = context.getOptionalValue(nextFullName, conv).orElse(null);
                }
            }
            map.put(mapKey, mapValue);
        }
        return map;
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

    private static boolean isSupportedRootOrGroupClass(Class<?> clazz) {
        if (clazz.getAnnotation(ConfigRoot.class) != null || clazz.getAnnotation(ConfigGroup.class) != null) {
            return true;
        }

        // Legacy code; we might want to remove this at some point....
        final String klassName = clazz.getName();
        for (final String supportedSuffix : SUPPORTED_CLASS_NAME_SUFFIXES) {
            if (klassName.endsWith(supportedSuffix)) {
                return true;
            }
        }
        return false;
    }

    private static class InstantiationContext {

        public static InstantiationContext withEmptyConfigSource() {
            var config = (SmallRyeConfig) ConfigProvider.getConfig();
            return new InstantiationContext(config, true, List.of());
        }

        public static InstantiationContext withCurrentConfigSource() {
            var config = (SmallRyeConfig) ConfigProvider.getConfig();
            var propertyNames = new ArrayList<String>(50);
            for (String name : config.getPropertyNames()) {
                if (name.startsWith(QUARKUS_PROPERTY_PREFIX)) {
                    propertyNames.add(name);
                }
            }
            return new InstantiationContext(config, false, propertyNames);
        }

        private final SmallRyeConfig config;
        private final boolean assumeEmptyConfigSource;
        private final List<String> quarkusPropertyNames;

        private InstantiationContext(SmallRyeConfig config, boolean assumeEmptyConfigSource,
                List<String> quarkusPropertyNames) {
            this.config = config;
            this.assumeEmptyConfigSource = assumeEmptyConfigSource;
            this.quarkusPropertyNames = quarkusPropertyNames;
        }

        <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
            if (assumeEmptyConfigSource) {
                return Converters.newOptionalConverter(converter).convert("");
            }
            return config.getOptionalValue(name, converter);
        }

        @SuppressWarnings("unchecked")
        Converter<?> getConverterFor(Type type) {
            // hopefully this is enough
            Class<?> rawType = rawTypeOf(type);
            if (Enum.class.isAssignableFrom(rawType)) {
                return new HyphenateEnumConverter(rawType);
            } else if (rawType == Optional.class) {
                return Converters.newOptionalConverter(getConverterFor(typeOfParameter(type, 0)));
            } else if (rawType == List.class) {
                return Converters.newCollectionConverter(getConverterFor(typeOfParameter(type, 0)), ArrayList::new);
            } else {
                return config.requireConverter(rawTypeOf(type));
            }
        }
    }
}
