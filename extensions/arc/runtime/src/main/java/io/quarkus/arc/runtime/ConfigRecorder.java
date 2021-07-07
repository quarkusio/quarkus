package io.quarkus.arc.runtime;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.impl.ParameterizedTypeImpl;
import io.quarkus.runtime.annotations.RecordableConstructor;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.inject.ConfigProducerUtil;

/**
 * @author Martin Kouba
 */
@Recorder
public class ConfigRecorder {

    public void validateConfigProperties(Set<ConfigValidationMetadata> properties) {
        Config config = ConfigProvider.getConfig();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ConfigRecorder.class.getClassLoader();
        }

        for (ConfigValidationMetadata property : properties) {
            Class<?> propertyType = load(property.getRawTypeName(), cl);
            Type effectivePropertyType = propertyType;
            // For parameterized types and arrays, we only check if the property config exists without trying to convert it
            if (propertyType.isArray() || (propertyType.getTypeParameters().length > 0 && propertyType != Map.class
                    && propertyType != List.class && propertyType != Set.class)) {
                effectivePropertyType = String.class;
            } else if (property.getActualTypeArgumentNames().size() > 0) {
                // this is a really simplified way of constructing the generic types, but we don't need anything more complex
                // here due to what SR Config checks (which is basically if the type is a collection)
                Type[] genericTypes = new Type[(property.getActualTypeArgumentNames().size())];
                for (int i = 0; i < property.getActualTypeArgumentNames().size(); i++) {
                    genericTypes[i] = load(property.getActualTypeArgumentNames().get(i), cl);
                }
                effectivePropertyType = new ParameterizedTypeImpl(propertyType, genericTypes);
            }

            try {
                ConfigProducerUtil.getValue(property.getName(), effectivePropertyType, property.getDefaultValue(), config);
            } catch (Exception e) {
                throw new DeploymentException(
                        "Failed to load config value of type " + effectivePropertyType + " for: " + property.getName(), e);
            }
        }
    }

    public void registerConfigMappings(final Set<ConfigClassWithPrefix> configClasses) {
        try {
            SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
            ConfigMappings.registerConfigMappings(config, configClasses);
        } catch (ConfigValidationException e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    public void registerConfigProperties(final Set<ConfigClassWithPrefix> configClasses) {
        try {
            SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
            ConfigMappings.registerConfigProperties(config, configClasses);
        } catch (ConfigValidationException e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    private Class<?> load(String className, ClassLoader cl) {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            default:
                try {
                    return Class.forName(className, true, cl);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Unable to load the config property type: " + className, e);
                }
        }
    }

    public static class ConfigValidationMetadata {
        private String name;
        private String rawTypeName;
        private List<String> actualTypeArgumentNames;
        private String defaultValue;

        @RecordableConstructor
        public ConfigValidationMetadata(final String name, final String rawTypeName, List<String> actualTypeArgumentNames,
                final String defaultValue) {
            this.name = name;
            this.rawTypeName = rawTypeName;
            this.actualTypeArgumentNames = actualTypeArgumentNames;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getRawTypeName() {
            return rawTypeName;
        }

        public List<String> getActualTypeArgumentNames() {
            return actualTypeArgumentNames;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ConfigValidationMetadata that = (ConfigValidationMetadata) o;
            return name.equals(that.name) && rawTypeName.equals(that.rawTypeName)
                    && actualTypeArgumentNames.equals(that.actualTypeArgumentNames)
                    && Objects.equals(defaultValue, that.defaultValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, rawTypeName, actualTypeArgumentNames, defaultValue);
        }
    }
}
