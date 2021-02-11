package io.quarkus.config.yaml.runtime;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.Converter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

/**
 * This class is used by generated code to support the list of objects use case in {@code @ConfigProperties}
 */
@SuppressWarnings("unused")
public abstract class AbstractYamlObjectConverter<T> implements Converter<T> {

    protected abstract Class<T> getClazz();

    /**
     * Contains names of fields that need to be converted from the value that MP-Config has set
     * to the actual name of the field in the class
     */
    protected Map<String, String> getFieldNameMap() {
        return Collections.emptyMap();
    }

    @Override
    public T convert(String value) {
        org.yaml.snakeyaml.constructor.Constructor constructor = new Constructor();
        CustomPropertyUtils propertyUtils = new CustomPropertyUtils(getFieldNameMap());
        propertyUtils.setSkipMissingProperties(true);
        constructor.setPropertyUtils(propertyUtils);
        return new Yaml(constructor).loadAs(value, getClazz());
    }

    private static class CustomPropertyUtils extends PropertyUtils {

        private final Map<String, String> fieldNameMap;

        public CustomPropertyUtils(Map<String, String> fieldNameMap) {
            this.fieldNameMap = fieldNameMap;
        }

        @Override
        public Property getProperty(Class<?> type, String name) {
            if (fieldNameMap.containsKey(name)) {
                name = fieldNameMap.get(name);
            }
            return super.getProperty(type, name);
        }
    }
}
