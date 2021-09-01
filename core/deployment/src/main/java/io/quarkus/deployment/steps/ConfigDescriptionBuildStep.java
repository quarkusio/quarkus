package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.configuration.matching.ConfigPatternMap;
import io.quarkus.deployment.configuration.matching.Container;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.util.ClassPathUtils;

public class ConfigDescriptionBuildStep {

    @BuildStep
    List<ConfigDescriptionBuildItem> createConfigDescriptions(
            ConfigurationBuildItem config) throws Exception {
        Properties javadoc = new Properties();
        ClassPathUtils.consumeAsStreams("META-INF/quarkus-javadoc.properties", in -> {
            try {
                javadoc.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        List<ConfigDescriptionBuildItem> ret = new ArrayList<>();
        processConfig(config.getReadResult().getBuildTimePatternMap(), ret, javadoc);
        processConfig(config.getReadResult().getBuildTimeRunTimePatternMap(), ret, javadoc);
        processConfig(config.getReadResult().getBootstrapPatternMap(), ret, javadoc);
        processConfig(config.getReadResult().getRunTimePatternMap(), ret, javadoc);
        return ret;
    }

    private void processConfig(ConfigPatternMap<Container> patterns, List<ConfigDescriptionBuildItem> ret,
            Properties javadoc) {

        patterns.forEach(new Consumer<Container>() {
            @Override
            public void accept(Container node) {
                Field field = node.findField();
                ConfigItem configItem = field.getAnnotation(ConfigItem.class);
                final ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
                String defaultDefault;
                final Class<?> valueClass = field.getType();

                EffectiveConfigTypeAndValues effectiveConfigTypeAndValues = getTypeName(field);

                if (valueClass == boolean.class) {
                    defaultDefault = "false";
                } else if (valueClass.isPrimitive() && valueClass != char.class) {
                    defaultDefault = "0";
                } else {
                    defaultDefault = null;
                }
                String defVal = defaultDefault;
                if (configItem != null) {
                    final String itemDefVal = configItem.defaultValue();
                    if (!itemDefVal.equals(ConfigItem.NO_DEFAULT)) {
                        defVal = itemDefVal;
                    }
                } else if (configProperty != null) {
                    final String propDefVal = configProperty.defaultValue();
                    if (!propDefVal.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                        defVal = propDefVal;
                    }
                }
                String javadocKey = field.getDeclaringClass().getName().replace('$', '.') + '.' + field.getName();
                ret.add(new ConfigDescriptionBuildItem("quarkus." + node.getPropertyName(),
                        node.findEnclosingClass().getConfigurationClass(),
                        defVal,
                        javadoc.getProperty(javadocKey),
                        effectiveConfigTypeAndValues.getTypeName(),
                        effectiveConfigTypeAndValues.getAllowedValues()));
            }
        });
    }

    private EffectiveConfigTypeAndValues getTypeName(Field field) {
        final Class<?> valueClass = field.getType();
        return getTypeName(field, valueClass);
    }

    private EffectiveConfigTypeAndValues getTypeName(Field field, Class<?> valueClass) {
        EffectiveConfigTypeAndValues typeAndValues = new EffectiveConfigTypeAndValues();
        String name = valueClass.getName();

        // Extract Optionals, Lists and Sets
        if ((valueClass.equals(Optional.class) || valueClass.equals(List.class) || valueClass.equals(Set.class))) {

            if (field != null) {
                Type genericType = field.getGenericType();
                name = genericType.getTypeName();
            }

            if (name.contains("<") && name.contains(">")) {
                name = name.substring(name.lastIndexOf("<") + 1, name.indexOf(">"));
            }

            try {
                Class<?> c = Class.forName(name);
                return getTypeName(null, c);
            } catch (ClassNotFoundException ex) {
                // Then we use the name as is.
            }
        }

        // Check other optionals
        if (valueClass.equals(OptionalInt.class)) {
            name = Integer.class.getName();
        } else if (valueClass.equals(OptionalDouble.class)) {
            name = Double.class.getName();
        } else if (valueClass.equals(OptionalLong.class)) {
            name = Long.class.getName();
        }

        // Check if this is an enum
        if (Enum.class.isAssignableFrom(valueClass)) {
            name = Enum.class.getName();

            Object[] values = valueClass.getEnumConstants();
            for (Object v : values) {
                Enum casted = (Enum) valueClass.cast(v);
                typeAndValues.addAllowedValue(casted.name());
            }
        }

        // Special case for Log level
        if (valueClass.isAssignableFrom(Level.class)) {
            typeAndValues.addAllowedValue(Level.ALL.getName());
            typeAndValues.addAllowedValue(Level.CONFIG.getName());
            typeAndValues.addAllowedValue(Level.FINE.getName());
            typeAndValues.addAllowedValue(Level.FINER.getName());
            typeAndValues.addAllowedValue(Level.FINEST.getName());
            typeAndValues.addAllowedValue(Level.INFO.getName());
            typeAndValues.addAllowedValue(Level.OFF.getName());
            typeAndValues.addAllowedValue(Level.SEVERE.getName());
            typeAndValues.addAllowedValue(Level.WARNING.getName());
        }

        // Map all primatives 
        if (name.equals("int")) {
            name = Integer.class.getName();
        } else if (name.equals("boolean")) {
            name = Boolean.class.getName();
        } else if (name.equals("float")) {
            name = Float.class.getName();
        } else if (name.equals("double")) {
            name = Double.class.getName();
        } else if (name.equals("long")) {
            name = Long.class.getName();
        } else if (name.equals("byte")) {
            name = Byte.class.getName();
        } else if (name.equals("short")) {
            name = Short.class.getName();
        } else if (name.equals("char")) {
            name = Character.class.getName();
        }

        typeAndValues.setTypeName(name);
        return typeAndValues;
    }

    class EffectiveConfigTypeAndValues {
        private String typeName;
        private List<String> allowedValues;

        public EffectiveConfigTypeAndValues() {

        }

        public EffectiveConfigTypeAndValues(String typeName) {
            this.typeName = typeName;
        }

        public EffectiveConfigTypeAndValues(String typeName, List<String> allowedValues) {
            this.typeName = typeName;
            this.allowedValues = allowedValues;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public List<String> getAllowedValues() {
            return allowedValues;
        }

        public void setAllowedValues(List<String> allowedValues) {
            this.allowedValues = allowedValues;
        }

        public void addAllowedValue(String v) {
            if (allowedValues == null) {
                allowedValues = new ArrayList<>();
            }
            allowedValues.add(v);
        }
    }

}
