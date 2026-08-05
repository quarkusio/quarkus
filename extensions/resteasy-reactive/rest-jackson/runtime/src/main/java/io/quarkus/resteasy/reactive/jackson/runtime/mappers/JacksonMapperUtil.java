package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.resteasy.reactive.jackson.runtime.security.RolesAllowedConfigExpStorage;
import io.quarkus.security.identity.SecurityIdentity;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.filter.FilteringGeneratorDelegate;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.util.NameTransformer;

public class JacksonMapperUtil {

    private static final AnnotatedMethod VISIBILITY_TEST_METHOD;
    static {
        try {
            Method m = Object.class.getMethod("getClass");
            VISIBILITY_TEST_METHOD = new AnnotatedMethod(null, m, null, null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPublicGetterVisible(SerializationContext context) {
        return context.getConfig().getDefaultVisibilityChecker()
                .isGetterVisible(VISIBILITY_TEST_METHOD);
    }

    public static boolean isPublicIsGetterVisible(SerializationContext context) {
        return context.getConfig().getDefaultVisibilityChecker()
                .isIsGetterVisible(VISIBILITY_TEST_METHOD);
    }

    public static void serializeBooleanAsNumber(boolean value, JsonGenerator generator) {
        generator.writeNumber(value ? 1 : 0);
    }

    public static void serializeDateAsTimestamp(Date value, JsonGenerator generator) {
        if (value == null) {
            generator.writeNull();
            return;
        }
        generator.writeNumber(value.getTime());
    }

    public static void serializeFormattedDate(Object value, String pattern, String timezone,
            JsonGenerator generator) {
        if (value == null) {
            generator.writeNull();
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        if (timezone != null) {
            sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        }
        generator.writeString(sdf.format(value));
    }

    public static void serializeFormattedTemporal(Object value, String pattern, String timezone,
            JsonGenerator generator) {
        if (value == null) {
            generator.writeNull();
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        if (timezone != null) {
            formatter = formatter.withZone(ZoneId.of(timezone));
        }
        generator.writeString(formatter.format((TemporalAccessor) value));
    }

    public static void serializeTemporalAsTimestamp(Object value, JsonGenerator generator) {
        if (value == null) {
            generator.writeNull();
        } else if (value instanceof Instant i) {
            generator.writeNumber(toBigDecimal(i.getEpochSecond(), i.getNano()));
        } else if (value instanceof ZonedDateTime zdt) {
            Instant inst = zdt.toInstant();
            generator.writeNumber(toBigDecimal(inst.getEpochSecond(), inst.getNano()));
        } else if (value instanceof OffsetDateTime odt) {
            Instant inst = odt.toInstant();
            generator.writeNumber(toBigDecimal(inst.getEpochSecond(), inst.getNano()));
        } else if (value instanceof Duration d) {
            generator.writeNumber(toBigDecimal(d.getSeconds(), d.getNano()));
        } else {
            generator.writeString(value.toString());
        }
    }

    private static BigDecimal toBigDecimal(long seconds, int nanoseconds) {
        if (nanoseconds == 0) {
            return BigDecimal.valueOf(seconds);
        }
        return new BigDecimal(seconds + "." + String.format("%09d", nanoseconds));
    }

    public static boolean isViewIncluded(Class<?> activeView, Class<?>[] viewClasses) {
        if (activeView == null) {
            return true;
        }
        for (Class<?> viewClass : viewClasses) {
            if (viewClass.isAssignableFrom(activeView)) {
                return true;
            }
        }
        return false;
    }

    public static boolean includeSecureField(SerializationContext serializationContext, String[] rolesAllowed) {
        return serializationContext.getFilterProvider() == null || includeSecureField(rolesAllowed);
    }

    public static boolean includeSecureField(String[] rolesAllowed) {
        SecurityIdentity securityIdentity = RolesAllowedHolder.SECURITY_IDENTITY;
        if (securityIdentity == null) {
            return false;
        }

        RolesAllowedConfigExpStorage rolesConfigExpStorage = RolesAllowedHolder.ROLES_ALLOWED_CONFIG_EXP_STORAGE;
        for (String role : rolesAllowed) {
            if (rolesConfigExpStorage != null) {
                // role config expression => resolved roles
                String[] roles = rolesConfigExpStorage.getRoles(role);
                if (roles != null) {
                    for (String r : roles) {
                        if (securityIdentity.hasRole(r)) {
                            return true;
                        }
                    }
                    continue;
                }
                // at this point, we know 'role' is not a configuration expression
            }
            if (securityIdentity.hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a field name to the JSON generator, translating through the ObjectMapper's
     * {@link PropertyNamingStrategy} if one is configured.
     * When no strategy is set, the pre-encoded {@code defaultName} is used for zero overhead.
     */
    public static void writeFieldName(JsonGenerator gen, PropertyNamingStrategy strategy,
            String javaFieldName, SerializableString defaultName) {
        if (strategy == null) {
            gen.writeName(defaultName);
        } else {
            gen.writeName(strategy.nameForField(null, null, javaFieldName));
        }
    }

    /**
     * Builds a reverse-translation index mapping strategy-translated JSON field names back to
     * Java field names. Called once at the start of deserialization so that per-field lookups
     * are O(1) via {@link Map#getOrDefault} instead of O(n) scans.
     */
    public static Map<String, String> buildReverseNameIndex(PropertyNamingStrategy strategy,
            String[] translatableFieldNames) {
        Map<String, String> index = new HashMap<>();
        for (String javaName : translatableFieldNames) {
            index.put(strategy.nameForField(null, null, javaName), javaName);
        }
        return index;
    }

    /**
     * Determine the root type that should be used for serialization of generic types.
     * Returns the appropriate root type or {@code null} if default serialization should be used.
     */
    public static JavaType getGenericRootType(Type genericType, ObjectWriter defaultWriter) {
        // Jackson needs additional type information when serializing generic types, as discussed here:
        // https://github.com/FasterXML/jackson-databind/issues/336 and https://github.com/FasterXML/jackson-databind/issues/23
        // Parts of the code were taken from org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider
        // which was used in quarkus-resteasy to handle this situation.
        JavaType rootType = null;
        if (genericType != null) {
            /*
             * 10-Jan-2011, tatu: as per [JACKSON-456], it's not safe to just force root
             * type since it prevents polymorphic type serialization. Since we really
             * just need this for generics, let's only use generic type if it's truly
             * generic.
             */
            if (genericType.getClass() != Class.class) {
                rootType = defaultWriter.getTypeFactory().constructType(genericType);
                /*
                 * 26-Feb-2011, tatu: To help with [JACKSON-518], we better recognize cases where
                 * type degenerates back into "Object.class" (as is the case with plain TypeVariable,
                 * for example), and not use that.
                 */
                if (rootType.getRawClass() == Object.class) {
                    rootType = null;
                }
            }
        }

        return rootType;
    }

    private static class RolesAllowedHolder {

        private static final ArcContainer ARC_CONTAINER = Arc.container();

        private static final SecurityIdentity SECURITY_IDENTITY = createSecurityIdentity();

        private static final RolesAllowedConfigExpStorage ROLES_ALLOWED_CONFIG_EXP_STORAGE = createRolesAllowedConfigExpStorage();

        private static SecurityIdentity createSecurityIdentity() {
            if (ARC_CONTAINER == null) {
                return null;
            }
            InstanceHandle<SecurityIdentity> instance = ARC_CONTAINER.instance(SecurityIdentity.class);
            return instance.isAvailable() ? instance.get() : null;
        }

        private static RolesAllowedConfigExpStorage createRolesAllowedConfigExpStorage() {
            if (ARC_CONTAINER == null) {
                return null;
            }
            InstanceHandle<RolesAllowedConfigExpStorage> rolesAllowedConfigExpStorage = ARC_CONTAINER
                    .instance(RolesAllowedConfigExpStorage.class);
            return rolesAllowedConfigExpStorage.isAvailable() ? rolesAllowedConfigExpStorage.get() : null;
        }
    }

    public static JavaType[] getGenericsJavaTypes(DeserializationContext context, BeanProperty property) {
        JavaType wrapperType = property != null ? property.getType() : context.getContextualType();
        JavaType[] valueTypes = new JavaType[wrapperType.containedTypeCount()];
        for (int i = 0; i < valueTypes.length; i++) {
            valueTypes[i] = wrapperType.containedType(i);
        }
        return valueTypes;
    }

    public static void serializePojo(Object value, JsonGenerator generator, SerializationContext serializationContext) {
        serializePojo(value, null, generator, serializationContext);
    }

    public static void serializePojo(Object value, Object bean, JsonGenerator generator,
            SerializationContext serializationContext) {
        if (value == null || value instanceof Map) {
            generator.writePOJO(value);
            return;
        }
        if (value == bean && handleSelfReference(bean, generator, serializationContext)) {
            return;
        }
        ValueSerializer<Object> serializer = serializationContext.findTypedValueSerializer(value.getClass(), true);
        if (serializer != null) {
            serializer.serialize(value, generator, serializationContext);
        } else {
            generator.writePOJO(value);
        }
    }

    private static boolean handleSelfReference(Object bean, JsonGenerator generator,
            SerializationContext serializationContext) {
        if (!serializationContext.isEnabled(SerializationFeature.FAIL_ON_SELF_REFERENCES)) {
            return false;
        }
        if (serializationContext.isEnabled(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)) {
            generator.writeNull();
            return true;
        }
        throw DatabindException.from(generator,
                "Direct self-reference leading to cycle (through reference chain: " + bean.getClass().getName() + ")");
    }

    @SuppressWarnings("unchecked")
    public static void serializeCollection(Object value, Class<?> collectionClass, Class<?> elementClass,
            JsonGenerator generator, SerializationContext serializationContext) {
        if (value == null) {
            generator.writeNull();
            return;
        }
        JavaType collectionType = serializationContext.getTypeFactory()
                .constructCollectionType((Class<? extends Collection>) collectionClass, elementClass);
        ValueSerializer<Object> serializer = serializationContext.findValueSerializer(collectionType);
        serializer.serialize(value, generator, serializationContext);
    }

    public static void serializeUnwrapped(Object value, JsonGenerator generator,
            SerializationContext serializationContext, Set<String> ignoredProperties,
            String prefix, String suffix) {
        if (value == null) {
            return;
        }
        if (!ignoredProperties.isEmpty()) {
            generator = new FilteringGeneratorDelegate(generator, new TokenFilter() {
                @Override
                public TokenFilter includeProperty(String name) {
                    return ignoredProperties.contains(name) ? null : TokenFilter.INCLUDE_ALL;
                }
            }, TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH, true);
        }
        boolean hasTransform = !prefix.isEmpty() || !suffix.isEmpty();
        if (hasTransform) {
            generator = new PrefixSuffixGeneratorDelegate(generator, prefix, suffix);
        }
        ValueSerializer<Object> serializer = serializationContext.findValueSerializer(value.getClass());
        if (serializer instanceof GeneratedSerializer gs) {
            gs.serializeContent(value, generator, serializationContext);
        } else {
            NameTransformer transformer = hasTransform
                    ? NameTransformer.simpleTransformer(prefix, suffix)
                    : NameTransformer.NOP;
            serializer.unwrappingSerializer(transformer)
                    .serialize(value, generator, serializationContext);
        }
    }

    public static void collectUnwrappedFields(JsonNode root, String fieldName, String prefix, String suffix) {
        if (!(root instanceof ObjectNode objectNode)) {
            return;
        }
        ObjectNode inner = objectNode.objectNode();
        List<String> toRemove = new ArrayList<>();
        for (String name : objectNode.propertyNames()) {
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                String stripped = name.substring(prefix.length(), name.length() - suffix.length());
                if (!stripped.isEmpty()) {
                    inner.set(stripped, objectNode.get(name));
                    toRemove.add(name);
                }
            }
        }
        if (!inner.isEmpty()) {
            toRemove.forEach(objectNode::remove);
            objectNode.set(fieldName, inner);
        }
    }

    public static void serializeAnyGetterMap(Map<?, ?> map, JsonGenerator generator,
            SerializationContext serializationContext) {
        if (map == null) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            generator.writeName(String.valueOf(entry.getKey()));
            Object value = entry.getValue();
            if (value == null) {
                generator.writeNull();
            } else {
                serializePojo(value, null, generator, serializationContext);
            }
        }
    }

    public enum SerializationInclude {

        ALWAYS,
        NON_NULL,
        NON_ABSENT,
        NON_EMPTY;

        public static SerializationInclude decode(Object object, SerializationContext serializationContext) {
            JsonInclude.Include include = serializationContext.getDefaultPropertyInclusion(object.getClass())
                    .getValueInclusion();
            return switch (include) {
                case NON_EMPTY -> NON_EMPTY;
                case NON_NULL -> NON_NULL;
                case NON_ABSENT -> NON_ABSENT;
                default -> ALWAYS;
            };
        }

        public boolean shouldSerialize(Object value) {
            return switch (this) {
                case ALWAYS -> true;
                case NON_NULL -> value != null;
                case NON_ABSENT -> isPresent(value);
                case NON_EMPTY -> hasValue(value);
            };
        }

        private boolean isPresent(Object value) {
            if (value == null) {
                return false;
            }
            if (value instanceof Optional o) {
                return o.isPresent();
            }
            return true;
        }

        private boolean hasValue(Object value) {
            if (!isPresent(value)) {
                return false;
            }
            if (value instanceof String s) {
                return !s.isEmpty();
            }
            if (value instanceof Collection c) {
                return !c.isEmpty();
            }
            if (value instanceof Map m) {
                return !m.isEmpty();
            }
            if (value.getClass().isArray()) {
                return Array.getLength(value) > 0;
            }
            return true;
        }
    }
}
