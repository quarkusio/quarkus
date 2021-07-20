package io.quarkus.arc.deployment;

import static io.quarkus.arc.deployment.ConfigClassBuildItem.Type.MAPPING;
import static io.quarkus.arc.deployment.ConfigClassBuildItem.Type.PROPERTIES;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;
import static org.jboss.jandex.AnnotationInstance.create;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.FIELD;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER;
import static org.jboss.jandex.AnnotationValue.createStringValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.CreationException;

import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.runtime.ConfigBeanCreator;
import io.quarkus.arc.runtime.ConfigMappingCreator;
import io.quarkus.arc.runtime.ConfigRecorder;
import io.quarkus.arc.runtime.ConfigRecorder.ConfigValidationMetadata;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingMetadata;
import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;
import io.smallrye.config.inject.ConfigProducer;

/**
 * MicroProfile Config related build steps.
 */
public class ConfigBuildStep {
    private static final DotName MP_CONFIG_PROPERTY_NAME = DotName.createSimple(ConfigProperty.class.getName());
    private static final DotName MP_CONFIG_PROPERTIES_NAME = DotName.createSimple(ConfigProperties.class.getName());
    private static final DotName MP_CONFIG_VALUE_NAME = DotName.createSimple(ConfigValue.class.getName());

    private static final DotName CONFIG_MAPPING_NAME = DotName.createSimple(ConfigMapping.class.getName());
    private static final DotName MAP_NAME = DotName.createSimple(Map.class.getName());
    private static final DotName SET_NAME = DotName.createSimple(Set.class.getName());
    private static final DotName LIST_NAME = DotName.createSimple(List.class.getName());
    private static final DotName SUPPLIER_NAME = DotName.createSimple(Supplier.class.getName());
    private static final DotName CONFIG_VALUE_NAME = DotName.createSimple(io.smallrye.config.ConfigValue.class.getName());
    public static final AnnotationInstance[] EMPTY_ANNOTATION_INSTANCES = {};

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(ConfigProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(ConfigProperties.class));
    }

    @BuildStep
    void analyzeConfigPropertyInjectionPoints(BeanDiscoveryFinishedBuildItem beanDiscovery,
            BuildProducer<ConfigPropertyBuildItem> configProperties,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        Set<Type> customBeanTypes = new HashSet<>();

        for (InjectionPointInfo injectionPoint : beanDiscovery.getInjectionPoints()) {
            if (injectionPoint.hasDefaultedQualifier()) {
                // Defaulted qualifier means no @ConfigProperty
                continue;
            }

            AnnotationInstance configProperty = injectionPoint.getRequiredQualifier(MP_CONFIG_PROPERTY_NAME);
            if (configProperty != null) {
                AnnotationValue nameValue = configProperty.value("name");
                AnnotationValue defaultValue = configProperty.value("defaultValue");
                String propertyName;
                if (nameValue != null) {
                    propertyName = nameValue.asString();
                } else {
                    // org.acme.Foo.config
                    if (injectionPoint.isField()) {
                        FieldInfo field = injectionPoint.getTarget().asField();
                        propertyName = getPropertyName(field.name(), field.declaringClass());
                    } else if (injectionPoint.isParam()) {
                        MethodInfo method = injectionPoint.getTarget().asMethod();
                        propertyName = getPropertyName(method.parameterName(injectionPoint.getPosition()),
                                method.declaringClass());
                    } else {
                        throw new IllegalStateException("Unsupported injection point target: " + injectionPoint);
                    }
                }

                // Register a custom bean for injection points that are not handled by ConfigProducer
                Type requiredType = injectionPoint.getRequiredType();
                if (!isHandledByProducers(requiredType)) {
                    customBeanTypes.add(requiredType);
                }

                if (DotNames.OPTIONAL.equals(requiredType.name())
                        || DotNames.OPTIONAL_INT.equals(requiredType.name())
                        || DotNames.OPTIONAL_LONG.equals(requiredType.name())
                        || DotNames.OPTIONAL_DOUBLE.equals(requiredType.name())
                        || DotNames.PROVIDER.equals(requiredType.name())
                        || SUPPLIER_NAME.equals(requiredType.name())
                        || CONFIG_VALUE_NAME.equals(requiredType.name())
                        || MP_CONFIG_VALUE_NAME.equals(requiredType.name())) {
                    // Never validate container objects
                    continue;
                }

                String propertyDefaultValue = null;
                if (defaultValue != null && (ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue.asString())
                        || !"".equals(defaultValue.asString()))) {
                    propertyDefaultValue = defaultValue.asString();
                }

                configProperties.produce(new ConfigPropertyBuildItem(propertyName, requiredType, propertyDefaultValue));
            }
        }

        for (Type type : customBeanTypes) {
            if (type.kind() != Kind.ARRAY) {
                // Implicit converters are most likely used
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, type.name().toString()));
            }
            DotName implClazz = type.kind() == Kind.ARRAY ? DotName.createSimple(ConfigBeanCreator.class.getName())
                    : type.name();
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(implClazz)
                    .creator(ConfigBeanCreator.class)
                    .providerType(type)
                    .types(type)
                    .addQualifier(MP_CONFIG_PROPERTY_NAME)
                    .param("requiredType", type.name().toString()).done());
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void validateConfigProperties(ConfigRecorder recorder, List<ConfigPropertyBuildItem> configProperties,
            BeanContainerBuildItem beanContainer, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // IMPL NOTE: we do depend on BeanContainerBuildItem to make sure that the BeanDeploymentValidator finished its processing

        // the non-primitive types need to be registered for reflection since Class.forName is used at runtime to load the class
        for (ConfigPropertyBuildItem item : configProperties) {
            Type requiredType = item.getPropertyType();
            String propertyType = requiredType.name().toString();
            if (requiredType.kind() != Kind.PRIMITIVE) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, propertyType));
            }
        }

        Set<ConfigValidationMetadata> propertiesToValidate = new HashSet<>();
        for (ConfigPropertyBuildItem configProperty : configProperties) {
            propertiesToValidate.add(new ConfigValidationMetadata(configProperty.getPropertyName(),
                    configProperty.getPropertyType().name().toString(),
                    configProperty.getDefaultValue()));
        }

        recorder.validateConfigProperties(propertiesToValidate);
    }

    @BuildStep
    void registerConfigRootsAsBeans(ConfigurationBuildItem configItem, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        for (RootDefinition rootDefinition : configItem.getReadResult().getAllRoots()) {
            if (rootDefinition.getConfigPhase() == ConfigPhase.BUILD_AND_RUN_TIME_FIXED
                    || rootDefinition.getConfigPhase() == ConfigPhase.RUN_TIME) {
                Class<?> configRootClass = rootDefinition.getConfigurationClass();
                syntheticBeans.produce(SyntheticBeanBuildItem.configure(configRootClass).types(configRootClass)
                        .scope(Dependent.class).creator(mc -> {
                            // e.g. return Config.ApplicationConfig
                            ResultHandle configRoot = mc.readStaticField(rootDefinition.getDescriptor());
                            // BUILD_AND_RUN_TIME_FIXED roots are always set before the container is started (in the static initializer of the generated Config class)
                            // However, RUN_TIME roots may be not be set when the bean instance is created
                            mc.ifNull(configRoot).trueBranch().throwException(CreationException.class,
                                    String.format("Config root [%s] with config phase [%s] not initialized yet.",
                                            configRootClass.getName(), rootDefinition.getConfigPhase().name()));
                            mc.returnValue(configRoot);
                        }).done());
            }
        }
    }

    @BuildStep
    AnnotationsTransformerBuildItem vetoMPConfigProperties() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return CLASS.equals(kind);
            }

            public void transform(TransformationContext context) {
                if (context.getAnnotations().stream()
                        .anyMatch(annotation -> annotation.name().equals(MP_CONFIG_PROPERTIES_NAME))) {
                    context.transform()
                            .add(DotNames.VETOED)
                            .add(DotNames.UNREMOVABLE)
                            .done();
                }
            }
        });
    }

    @BuildStep
    void generateConfigClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ConfigClassBuildItem> configClasses) {

        List<AnnotationInstance> mappingAnnotations = new ArrayList<>();
        mappingAnnotations.addAll(combinedIndex.getIndex().getAnnotations(CONFIG_MAPPING_NAME));
        mappingAnnotations.addAll(combinedIndex.getIndex().getAnnotations(MP_CONFIG_PROPERTIES_NAME));

        for (AnnotationInstance instance : mappingAnnotations) {
            AnnotationTarget target = instance.target();
            AnnotationValue annotationPrefix = instance.value("prefix");

            if (target.kind().equals(FIELD)) {
                if (annotationPrefix != null && !annotationPrefix.asString().equals(UNCONFIGURED_PREFIX)) {
                    configClasses.produce(
                            toConfigClassBuildItem(instance, toClass(target.asField().type().name()),
                                    annotationPrefix.asString()));
                    continue;
                }
            }

            if (target.kind().equals(METHOD_PARAMETER)) {
                if (annotationPrefix != null && !annotationPrefix.asString().equals(UNCONFIGURED_PREFIX)) {
                    ClassType classType = target.asMethodParameter().method().parameters()
                            .get(target.asMethodParameter().position()).asClassType();
                    configClasses
                            .produce(toConfigClassBuildItem(instance, toClass(classType.name()), annotationPrefix.asString()));
                    continue;
                }
            }

            if (!target.kind().equals(CLASS)) {
                continue;
            }

            Class<?> configClass = toClass(target.asClass().name());
            String prefix = Optional.ofNullable(annotationPrefix).map(AnnotationValue::asString).orElse("");

            List<ConfigMappingMetadata> configMappingsMetadata = ConfigMappingLoader.getConfigMappingsMetadata(configClass);
            Set<String> generatedClassesNames = new HashSet<>();
            Set<ClassInfo> mappingsInfo = new HashSet<>();
            configMappingsMetadata.forEach(mappingMetadata -> {
                generatedClasses.produce(
                        new GeneratedClassBuildItem(true, mappingMetadata.getClassName(), mappingMetadata.getClassBytes()));
                reflectiveClasses
                        .produce(ReflectiveClassBuildItem.builder(mappingMetadata.getInterfaceType()).methods(true).build());
                reflectiveClasses
                        .produce(ReflectiveClassBuildItem.builder(mappingMetadata.getClassName()).constructors(true).build());

                for (Class<?> parent : getHierarchy(mappingMetadata.getInterfaceType())) {
                    reflectiveClasses.produce(ReflectiveClassBuildItem.builder(parent).methods(true).build());
                }

                generatedClassesNames.add(mappingMetadata.getClassName());

                ClassInfo mappingInfo = combinedIndex.getIndex()
                        .getClassByName(DotName.createSimple(mappingMetadata.getInterfaceType().getName()));
                if (mappingInfo != null) {
                    mappingsInfo.add(mappingInfo);
                }
            });

            // Search and register possible classes for implicit Converter methods
            for (ClassInfo classInfo : mappingsInfo) {
                for (MethodInfo method : classInfo.methods()) {
                    if (!isHandledByProducers(method.returnType()) &&
                            mappingsInfo.stream()
                                    .map(ClassInfo::name)
                                    .noneMatch(name -> name.equals(method.returnType().name()))) {
                        reflectiveClasses
                                .produce(new ReflectiveClassBuildItem(true, false, method.returnType().name().toString()));
                    }
                }
            }

            configClasses.produce(toConfigClassBuildItem(instance, configClass, generatedClassesNames, prefix));
        }
    }

    @BuildStep
    void beanConfigClasses(
            List<ConfigClassBuildItem> configClasses,
            BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BeanConfiguratorBuildItem> beanConfigurationRegistry) {

        for (ConfigClassBuildItem configClass : configClasses) {
            if (configClass.getGeneratedClasses().isEmpty()) {
                continue;
            }

            List<AnnotationInstance> qualifiers = new ArrayList<>();
            if (configClass.isProperties()) {
                qualifiers.add(
                        create(MP_CONFIG_PROPERTIES_NAME, null,
                                new AnnotationValue[] { createStringValue("prefix", configClass.getPrefix()) }));
            }

            collectTypes(combinedIndex, configClass);

            beanConfigurationRegistry.produce(new BeanConfiguratorBuildItem(
                    beanRegistrationPhase.getContext()
                            .configure(configClass.getConfigClass())
                            .types(collectTypes(combinedIndex, configClass))
                            .qualifiers(qualifiers.toArray(EMPTY_ANNOTATION_INSTANCES))
                            .creator(ConfigMappingCreator.class)
                            .param("type", configClass.getConfigClass())
                            .param("prefix", configClass.getPrefix())));
        }
    }

    private Type[] collectTypes(CombinedIndexBuildItem combinedIndex, ConfigClassBuildItem configClass) {
        IndexView index = combinedIndex.getIndex();
        DotName configIfaceName = DotName.createSimple(configClass.getConfigClass().getName());
        ClassInfo configIfaceInfo = index.getClassByName(configIfaceName);
        if ((configIfaceInfo == null) || configIfaceInfo.interfaceNames().isEmpty()) {
            return new Type[] { Type.create(configIfaceName, Kind.CLASS) };
        }

        Set<DotName> allIfaces = new HashSet<>();
        allIfaces.add(configIfaceName);
        collectInterfacesRec(configIfaceInfo, index, allIfaces);
        Type[] result = new Type[allIfaces.size()];
        int i = 0;
        for (DotName iface : allIfaces) {
            result[i++] = Type.create(iface, Kind.CLASS);
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

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerConfigClasses(
            RecorderContext context,
            ConfigRecorder recorder,
            List<ConfigClassBuildItem> configClasses) throws Exception {

        context.registerNonDefaultConstructor(
                ConfigClassWithPrefix.class.getDeclaredConstructor(Class.class, String.class),
                configClassWithPrefix -> Stream.of(configClassWithPrefix.getKlass(), configClassWithPrefix.getPrefix())
                        .collect(toList()));

        recorder.registerConfigMappings(
                configClasses.stream()
                        .filter(ConfigClassBuildItem::isMapping)
                        .map(configMapping -> configClassWithPrefix(configMapping.getConfigClass(), configMapping.getPrefix()))
                        .collect(toSet()));

        recorder.registerConfigProperties(
                configClasses.stream()
                        .filter(ConfigClassBuildItem::isProperties)
                        .map(configProperties -> configClassWithPrefix(configProperties.getConfigClass(),
                                configProperties.getPrefix()))
                        .collect(toSet()));
    }

    private static Class<?> toClass(DotName dotName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return classLoader.loadClass(dotName.toString());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The class (" + dotName.toString() + ") cannot be created during deployment.", e);
        }
    }

    private static ConfigClassBuildItem toConfigClassBuildItem(
            AnnotationInstance instance,
            Class<?> configClass,
            String prefix) {
        return toConfigClassBuildItem(instance, configClass, emptySet(), prefix);
    }

    private static ConfigClassBuildItem toConfigClassBuildItem(
            AnnotationInstance instance,
            Class<?> configClass,
            Set<String> generatedClasses,
            String prefix) {
        if (instance.name().equals(CONFIG_MAPPING_NAME)) {
            return new ConfigClassBuildItem(configClass, generatedClasses, prefix, MAPPING);
        } else if (instance.name().equals(MP_CONFIG_PROPERTIES_NAME)) {
            return new ConfigClassBuildItem(configClass, generatedClasses, prefix, PROPERTIES);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static List<Class<?>> getHierarchy(Class<?> mapping) {
        List<Class<?>> interfaces = new ArrayList<>();
        for (Class<?> i : mapping.getInterfaces()) {
            interfaces.add(i);
            interfaces.addAll(getHierarchy(i));
        }
        return interfaces;
    }

    private String getPropertyName(String name, ClassInfo declaringClass) {
        StringBuilder builder = new StringBuilder();
        if (declaringClass.enclosingClass() == null) {
            builder.append(declaringClass.name());
        } else {
            builder.append(declaringClass.enclosingClass()).append(".").append(declaringClass.simpleName());
        }
        return builder.append(".").append(name).toString();
    }

    public static boolean isHandledByProducers(Type type) {
        if (type.kind() == Kind.ARRAY) {
            return false;
        }
        if (type.kind() == Kind.PRIMITIVE) {
            return true;
        }
        return DotNames.STRING.equals(type.name()) ||
                DotNames.OPTIONAL.equals(type.name()) ||
                DotNames.OPTIONAL_INT.equals(type.name()) ||
                DotNames.OPTIONAL_LONG.equals(type.name()) ||
                DotNames.OPTIONAL_DOUBLE.equals(type.name()) ||
                MAP_NAME.equals(type.name()) ||
                SET_NAME.equals(type.name()) ||
                LIST_NAME.equals(type.name()) ||
                DotNames.LONG.equals(type.name()) ||
                DotNames.FLOAT.equals(type.name()) ||
                DotNames.INTEGER.equals(type.name()) ||
                DotNames.BOOLEAN.equals(type.name()) ||
                DotNames.DOUBLE.equals(type.name()) ||
                DotNames.SHORT.equals(type.name()) ||
                DotNames.BYTE.equals(type.name()) ||
                DotNames.CHARACTER.equals(type.name()) ||
                SUPPLIER_NAME.equals(type.name()) ||
                CONFIG_VALUE_NAME.equals(type.name()) ||
                MP_CONFIG_VALUE_NAME.equals(type.name());
    }
}
