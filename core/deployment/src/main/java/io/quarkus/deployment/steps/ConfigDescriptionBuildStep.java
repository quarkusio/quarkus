package io.quarkus.deployment.steps;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED;
import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;
import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;

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
        processConfig(config.getReadResult().getBuildTimePatternMap(), ret, javadoc, BUILD_TIME);
        processConfig(config.getReadResult().getBuildTimeRunTimePatternMap(), ret, javadoc, BUILD_AND_RUN_TIME_FIXED);
        processConfig(config.getReadResult().getRunTimePatternMap(), ret, javadoc, RUN_TIME);
        processMappings(config.getReadResult().getBuildTimeMappings(), ret, javadoc, BUILD_TIME);
        processMappings(config.getReadResult().getBuildTimeRunTimeMappings(), ret, javadoc, BUILD_AND_RUN_TIME_FIXED);
        processMappings(config.getReadResult().getRunTimeMappings(), ret, javadoc, RUN_TIME);
        return ret;
    }

    private void processConfig(ConfigPatternMap<Container> patterns, List<ConfigDescriptionBuildItem> ret, Properties javadoc,
            ConfigPhase configPhase) {
        for (String childName : patterns.childNames()) {
            ConfigPatternMap<Container> child = patterns.getChild(childName);
            processConfigChild(childName, child, ret, javadoc, configPhase);
        }
    }

    private void processConfigChild(String name, ConfigPatternMap<Container> patterns, List<ConfigDescriptionBuildItem> ret,
            Properties javadoc, ConfigPhase configPhase) {

        Iterable<String> childNames = patterns.childNames();
        if (childNames.iterator().hasNext()) {
            for (String childName : childNames) {
                ConfigPatternMap<Container> child = patterns.getChild(childName);
                processConfigChild(name + "." + childName, child, ret, javadoc, configPhase);
            }
        } else {
            patterns.forEach(new Consumer<Container>() {
                @Override
                public void accept(Container node) {
                    Field field = node.findField();
                    ConfigItem configItem = field.getAnnotation(ConfigItem.class);
                    final ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
                    String defaultDefault;
                    final Class<?> valueClass = field.getType();

                    EffectiveConfigTypeAndValues effectiveConfigTypeAndValues = getTypeName(valueClass, field.getGenericType());

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
                    ret.add(new ConfigDescriptionBuildItem(name,
                            defVal,
                            javadoc.getProperty(javadocKey),
                            effectiveConfigTypeAndValues.typeName(),
                            effectiveConfigTypeAndValues.allowedValues(),
                            configPhase));
                }
            });
        }
    }

    private void processMappings(List<ConfigClass> mappings, List<ConfigDescriptionBuildItem> descriptionBuildItems,
            Properties javaDocProperties, ConfigPhase configPhase) {
        for (ConfigClass mapping : mappings) {
            Map<String, Property> properties = ConfigMappings.getProperties(mapping);
            for (Map.Entry<String, Property> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Property property = entry.getValue();
                Method method = property.getMethod();

                String defaultValue = null;
                if (property instanceof PrimitiveProperty) {
                    PrimitiveProperty primitiveProperty = (PrimitiveProperty) property;
                    if (primitiveProperty.hasDefaultValue()) {
                        defaultValue = primitiveProperty.getDefaultValue();
                    } else if (primitiveProperty.getPrimitiveType() == boolean.class) {
                        defaultValue = "false";
                    } else if (primitiveProperty.getPrimitiveType() != char.class) {
                        defaultValue = "0";
                    }
                } else if (property instanceof LeafProperty) {
                    LeafProperty leafProperty = (LeafProperty) property;
                    if (leafProperty.hasDefaultValue()) {
                        defaultValue = leafProperty.getDefaultValue();
                    }
                }

                String javadocKey = method.getDeclaringClass().getName().replace('$', '.') + '.' + method.getName();
                EffectiveConfigTypeAndValues typeName = getTypeName(method.getReturnType(), method.getGenericReturnType());
                descriptionBuildItems.add(new ConfigDescriptionBuildItem(propertyName, defaultValue,
                        javaDocProperties.getProperty(javadocKey), typeName.typeName(), typeName.allowedValues(), configPhase));
            }
        }
    }

    private EffectiveConfigTypeAndValues getTypeName(Class<?> valueClass, Type genericType) {
        final String name;
        final List<String> allowedValues = new ArrayList<>();

        // Extract Optionals, Lists and Sets
        if ((valueClass.equals(Optional.class) || valueClass.equals(List.class) || valueClass.equals(Set.class))) {
            String thisName = valueClass.getName();
            if (genericType != null) {
                thisName = genericType.getTypeName();
            }

            if (thisName.contains("<") && thisName.contains(">")) {
                thisName = thisName.substring(thisName.lastIndexOf("<") + 1, thisName.indexOf(">"));
            }

            try {
                Class<?> c = Class.forName(thisName);
                return getTypeName(c, null);
            } catch (ClassNotFoundException ex) {
                // Then we use the name as is.
            }
            name = thisName;
        } else if (Enum.class.isAssignableFrom(valueClass)) {
            // Check if this is an enum
            name = Enum.class.getName();

            Object[] values = valueClass.getEnumConstants();
            for (Object v : values) {
                Enum<?> casted = (Enum<?>) valueClass.cast(v);
                allowedValues.add(casted.name());
            }
        } else {
            // Map all primitives
            name = switch (valueClass.getName()) {
                case "java.util.OptionalInt", "int" -> Integer.class.getName();
                case "boolean" -> Boolean.class.getName();
                case "float" -> Float.class.getName();
                case "java.util.OptionalDouble", "double" -> Double.class.getName();
                case "java.util.OptionalLong", "long" -> Long.class.getName();
                case "byte" -> Byte.class.getName();
                case "short" -> Short.class.getName();
                case "char" -> Character.class.getName();
                default -> valueClass.getName();
            };
        }

        // Special case for Log level
        if (valueClass.isAssignableFrom(Level.class)) {
            allowedValues.add(Level.ALL.getName());
            allowedValues.add(Level.CONFIG.getName());
            allowedValues.add(Level.FINE.getName());
            allowedValues.add(Level.FINER.getName());
            allowedValues.add(Level.FINEST.getName());
            allowedValues.add(Level.INFO.getName());
            allowedValues.add(Level.OFF.getName());
            allowedValues.add(Level.SEVERE.getName());
            allowedValues.add(Level.WARNING.getName());
        }

        return new EffectiveConfigTypeAndValues(name, allowedValues);
    }

    private record EffectiveConfigTypeAndValues(String typeName, List<String> allowedValues) {
    }
}
