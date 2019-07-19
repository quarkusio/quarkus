package io.quarkus.resteasy.jsonb.runtime.serializers;

import java.util.HashMap;
import java.util.Map;

import javax.json.bind.serializer.JsonbSerializer;

import org.eclipse.yasson.internal.Marshaller;
import org.eclipse.yasson.internal.model.PropertyModel;
import org.eclipse.yasson.internal.serializer.SerializerBuilder;

public final class UnhandledTypeGeneratorUtil {

    private static final Map<String, JsonbSerializer<?>> CACHE = new HashMap<>();

    private UnhandledTypeGeneratorUtil() {
    }

    /**
     * Use Yasson to generate a serializer for a property whose type we don't yet handle
     * <p>
     * The third param is a string to avoid having to load the class when it's not needed
     * <p>
     * used by UnhandledTypeGenerator
     */
    public static JsonbSerializer<?> getSerializerForUnhandledType(Marshaller marshaller, Class enclosingClass,
            Object propertyValue, String propertyName) throws ClassNotFoundException {
        PropertyModel propertyModel = marshaller.getMappingContext().getOrCreateClassModel(enclosingClass)
                .getPropertyModel(propertyName);
        JsonbSerializer<?> powerUnitSerializer = propertyModel.getPropertySerializer();
        if (powerUnitSerializer != null) {
            return powerUnitSerializer;
        }

        Class<?> propertyClass = propertyValue.getClass();
        String cacheKey = enclosingClass.getName() + "-" + propertyName;
        if (CACHE.containsKey(cacheKey)) {
            return CACHE.get(cacheKey);
        }

        powerUnitSerializer = new SerializerBuilder(marshaller.getJsonbContext())
                .withObjectClass(propertyClass)
                .withCustomization(propertyModel.getCustomization())
                .build();
        CACHE.put(cacheKey, powerUnitSerializer);
        return powerUnitSerializer;
    }
}
