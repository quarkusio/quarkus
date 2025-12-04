package io.quarkus.deployment.configuration;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.isApplicationClass;
import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigClassBuildItem;
import io.quarkus.deployment.builditem.ConfigClassBuildItem.Kind;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.hibernate.validator.spi.AdditionalConstrainedClassBuildItem;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappingInterface;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingMetadata;
import io.smallrye.config.ConfigMappings.ConfigClass;

public class ConfigMappingUtils {
    public static final DotName CONFIG_MAPPING_NAME = DotName.createSimple(ConfigMapping.class.getName());

    private ConfigMappingUtils() {
        throw new UnsupportedOperationException();
    }

    // Used for application Mappings and MP ConfigProperties
    public static void processConfigClasses(
            NativeConfig nativeConfig,
            ConfigurationBuildItem configItem,
            CombinedIndexBuildItem combinedIndex,
            Map<String, GeneratedClassBuildItem> generatedConfigClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ConfigClassBuildItem> configClasses,
            BuildProducer<AdditionalConstrainedClassBuildItem> additionalConstrainedClasses,
            DotName configAnnotation) {

        for (AnnotationInstance instance : combinedIndex.getIndex().getAnnotations(configAnnotation)) {
            AnnotationTarget target = instance.target();
            AnnotationValue annotationPrefix = instance.value("prefix");

            if (!target.kind().equals(CLASS)) {
                continue;
            }

            // Ignore scanned classes from the processor.
            // Usually they are not available in Jandex, but there are cases where extensions have jandex to their
            // own classes, or user applications adding @ConfigRoot to generate documentation. In that case, it would
            // generate duplicates.
            ConfigClass configClass = configClass(
                    toClass(target.asClass().name()),
                    Optional.ofNullable(annotationPrefix).map(AnnotationValue::asString).orElse(""));
            if (configItem.getReadResult().getAllMappingsByClass().containsKey(configClass.getType())) {
                continue;
            }
            Kind configClassKind = getConfigClassType(instance);
            processConfigClass(nativeConfig, configClass, configClassKind, combinedIndex, generatedConfigClasses,
                    reflectiveClasses,
                    reflectiveMethods, configClasses, additionalConstrainedClasses);
        }
    }

    public static void processConfigMapping(
            NativeConfig nativeConfig,
            ConfigurationBuildItem configItem,
            CombinedIndexBuildItem combinedIndex,
            Map<String, GeneratedClassBuildItem> generatedConfigClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ConfigClassBuildItem> configClasses,
            BuildProducer<AdditionalConstrainedClassBuildItem> additionalConstrainedClasses) {
        processConfigClasses(nativeConfig, configItem, combinedIndex, generatedConfigClasses, reflectiveClasses,
                reflectiveMethods,
                configClasses, additionalConstrainedClasses, CONFIG_MAPPING_NAME);
    }

    public static void processExtensionConfigMapping(
            NativeConfig nativeConfig,
            ConfigClass configClass,
            CombinedIndexBuildItem combinedIndex,
            Map<String, GeneratedClassBuildItem> generatedConfigClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ConfigClassBuildItem> configClasses,
            BuildProducer<AdditionalConstrainedClassBuildItem> additionalConstrainedClasses) {

        processConfigClass(nativeConfig, configClass, Kind.MAPPING, combinedIndex, generatedConfigClasses, reflectiveClasses,
                reflectiveMethods, configClasses, additionalConstrainedClasses);
    }

    private static void processConfigClass(
            NativeConfig nativeConfig,
            ConfigClass configClassWithPrefix,
            Kind configClassKind,
            CombinedIndexBuildItem combinedIndex,
            Map<String, GeneratedClassBuildItem> generatedConfigClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ConfigClassBuildItem> configClasses,
            BuildProducer<AdditionalConstrainedClassBuildItem> additionalConstrainedClasses) {

        Class<?> configClass = configClassWithPrefix.getType();
        String prefix = configClassWithPrefix.getPrefix();

        List<ConfigMappingMetadata> configMappingsMetadata = ConfigMappingLoader.getConfigMappingsMetadata(configClass);
        Set<String> generatedClassesNames = new HashSet<>();
        // all the config interfaces including nested ones
        Set<Class<?>> configComponentInterfaces = new HashSet<>();
        configMappingsMetadata.forEach(mappingMetadata -> {
            generatedClassesNames.add(mappingMetadata.getClassName());
            // This is the generated implementation of the mapping by SmallRye Config.
            byte[] classBytes = mappingMetadata.getClassBytes();
            generatedConfigClasses.put(mappingMetadata.getClassName(),
                    new GeneratedClassBuildItem(isApplicationClass(configClass.getName()), mappingMetadata.getClassName(),
                            classBytes));
            additionalConstrainedClasses.produce(AdditionalConstrainedClassBuildItem.of(mappingMetadata.getClassName(),
                    classBytes));
            ReflectiveClassBuildItem.Builder reflection = ReflectiveClassBuildItem.builder(mappingMetadata.getClassName());
            reflection.methods();
            reflectiveClasses.produce(reflection
                    .reason(ConfigMappingUtils.class.getName())
                    .build());
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(ConfigMappingUtils.class.getName(),
                    mappingMetadata.getClassName(), "getProperties", new String[0]));
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(ConfigMappingUtils.class.getName(),
                    mappingMetadata.getClassName(), "getSecrets", new String[0]));

            configComponentInterfaces.add(mappingMetadata.getInterfaceType());

            processProperties(mappingMetadata.getInterfaceType(), reflectiveClasses);
        });

        configClasses.produce(new ConfigClassBuildItem(configClass, configComponentInterfaces,
                collectTypes(combinedIndex, configClass),
                generatedClassesNames, prefix, configClassKind));
    }

    private static void processProperties(
            Class<?> configClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(configClass);
        for (Property property : mapping.getProperties()) {
            String reason = ConfigMappingUtils.class.getSimpleName() + " Required to process property "
                    + property.getPropertyName();

            if (property.hasConvertWith()) {
                Class<? extends Converter<?>> convertWith;
                if (property.isLeaf()) {
                    convertWith = property.asLeaf().getConvertWith();
                } else {
                    convertWith = property.asPrimitive().getConvertWith();
                }
                reflectiveClasses.produce(ReflectiveClassBuildItem.builder(convertWith).reason(reason).build());
            }

            registerImplicitConverter(property, reason, reflectiveClasses);

            if (property.isMap()) {
                MapProperty mapProperty = property.asMap();
                if (mapProperty.hasKeyConvertWith()) {
                    reflectiveClasses
                            .produce(ReflectiveClassBuildItem.builder(mapProperty.getKeyConvertWith()).reason(reason).build());
                } else {
                    reflectiveClasses
                            .produce(ReflectiveClassBuildItem.builder(mapProperty.getKeyRawType()).reason(reason).build());
                }

                registerImplicitConverter(mapProperty.getValueProperty(), reason, reflectiveClasses);
            }
        }
    }

    private static void registerImplicitConverter(
            Property property,
            String reason, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        if (property.isLeaf() && !property.isOptional()) {
            LeafProperty leafProperty = property.asLeaf();
            if (leafProperty.hasConvertWith()) {
                reflectiveClasses
                        .produce(ReflectiveClassBuildItem.builder(leafProperty.getConvertWith()).reason(reason).build());
            } else {
                reflectiveClasses
                        .produce(ReflectiveClassBuildItem.builder(leafProperty.getValueRawType()).reason(reason).methods()
                                .build());
            }
        } else if (property.isOptional()) {
            registerImplicitConverter(property.asOptional().getNestedProperty(), reason, reflectiveClasses);
        } else if (property.isCollection()) {
            registerImplicitConverter(property.asCollection().getElement(), reason, reflectiveClasses);
        }
    }

    @Deprecated(forRemoval = true, since = "3.25")
    public static Object newInstance(Class<?> configClass) {
        if (configClass.isAnnotationPresent(ConfigMapping.class)) {
            return ReflectUtil.newInstance(ConfigMappingLoader.ensureLoaded(configClass).implementation());
        } else {
            return ReflectUtil.newInstance(configClass);
        }
    }

    private static Class<?> toClass(DotName dotName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return classLoader.loadClass(dotName.toString());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The class (" + dotName + ") cannot be created during deployment.", e);
        }
    }

    private static Kind getConfigClassType(AnnotationInstance instance) {
        if (instance.name().equals(CONFIG_MAPPING_NAME)) {
            return Kind.MAPPING;
        } else {
            return Kind.PROPERTIES;
        }
    }

    private static Set<Type> collectTypes(CombinedIndexBuildItem combinedIndex, Class<?> configClass) {
        IndexView index = combinedIndex.getIndex();
        DotName configIfaceName = DotName.createSimple(configClass.getName());
        ClassInfo configIfaceInfo = index.getClassByName(configIfaceName);
        if ((configIfaceInfo == null) || configIfaceInfo.interfaceNames().isEmpty()) {
            return Set.of(Type.create(configIfaceName, Type.Kind.CLASS));
        }

        Set<DotName> allIfaces = new HashSet<>();
        allIfaces.add(configIfaceName);
        collectInterfacesRec(configIfaceInfo, index, allIfaces);
        Set<Type> result = new HashSet<>(allIfaces.size());
        for (DotName iface : allIfaces) {
            result.add(Type.create(iface, Type.Kind.CLASS));
        }
        return result;
    }

    private static void collectInterfacesRec(ClassInfo current, IndexView index, Set<DotName> result) {
        List<DotName> interfaces = current.interfaceNames();
        if (interfaces.isEmpty()) {
            return;
        }
        for (DotName iface : interfaces) {
            ClassInfo classByName = index.getClassByName(iface);
            if (classByName == null) {
                continue; // just ignore this type
            }
            result.add(iface);
            collectInterfacesRec(classByName, index, result);
        }
    }
}
