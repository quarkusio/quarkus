package io.quarkus.arc.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

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
            Class<?> propertyClass = load(property.getType(), cl);
            // For parameterized types and arrays, we only check if the property config exists without trying to convert it
            if (propertyClass.isArray() || (propertyClass.getTypeParameters().length > 0 && propertyClass != Map.class)) {
                propertyClass = String.class;
            }

            try {
                ConfigProducerUtil.getValue(property.getName(), propertyClass, property.getDefaultValue(), config);
            } catch (Exception e) {
                throw new DeploymentException(
                        "Failed to load config value of type " + propertyClass + " for: " + property.getName(), e);
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
        private String type;
        private String defaultValue;

        public ConfigValidationMetadata() {
        }

        public ConfigValidationMetadata(final String name, final String type, final String defaultValue) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ConfigValidationMetadata that = (ConfigValidationMetadata) o;
            return name.equals(that.name) &&
                    type.equals(that.type) &&
                    Objects.equals(defaultValue, that.defaultValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, defaultValue);
        }
    }
}
