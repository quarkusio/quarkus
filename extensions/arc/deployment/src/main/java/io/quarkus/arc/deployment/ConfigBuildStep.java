package io.quarkus.arc.deployment;

import static io.quarkus.arc.processor.Annotations.getParameterAnnotations;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.builditem.ConfigClassBuildItem.Kind.MAPPING;
import static io.quarkus.deployment.builditem.ConfigClassBuildItem.Kind.PROPERTIES;
import static io.quarkus.deployment.configuration.ConfigMappingUtils.CONFIG_MAPPING_NAME;
import static io.quarkus.deployment.configuration.ConfigMappingUtils.processConfigClasses;
import static io.smallrye.config.ConfigMappings.ConfigClassWithPrefix.configClassWithPrefix;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;
import static org.jboss.jandex.AnnotationInstance.create;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.FIELD;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;
import static org.jboss.jandex.AnnotationValue.createStringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.CreationException;

import org.eclipse.microprofile.config.Config;
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
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanConfigurator;
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
import io.quarkus.deployment.builditem.ConfigClassBuildItem;
import io.quarkus.deployment.builditem.ConfigMappingBuildItem;
import io.quarkus.deployment.builditem.ConfigPropertiesBuildItem;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.definition.RootDefinition;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.smallrye.config.ConfigMappings.ConfigClassWithPrefix;
import io.smallrye.config.inject.ConfigProducer;

/**
 * MicroProfile Config related build steps.
 */
public class ConfigBuildStep {
    private static final Logger LOGGER = Logger.getLogger(ConfigBuildStep.class.getName());

    private static final DotName MP_CONFIG = DotName.createSimple(Config.class.getName());
    private static final DotName MP_CONFIG_PROPERTY_NAME = DotName.createSimple(ConfigProperty.class.getName());
    private static final DotName MP_CONFIG_PROPERTIES_NAME = DotName.createSimple(ConfigProperties.class.getName());
    private static final DotName MP_CONFIG_VALUE_NAME = DotName.createSimple(ConfigValue.class.getName());

    private static final DotName SR_CONFIG = DotName.createSimple(io.smallrye.config.SmallRyeConfig.class.getName());
    private static final DotName SR_CONFIG_VALUE_NAME = DotName.createSimple(io.smallrye.config.ConfigValue.class.getName());

    private static final DotName MAP_NAME = DotName.createSimple(Map.class.getName());
    private static final DotName SET_NAME = DotName.createSimple(Set.class.getName());
    private static final DotName LIST_NAME = DotName.createSimple(List.class.getName());
    private static final DotName SUPPLIER_NAME = DotName.createSimple(Supplier.class.getName());

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(ConfigProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(ConfigProperties.class));
    }

    @BuildStep
    void registerCustomConfigBeanTypes(BeanDiscoveryFinishedBuildItem beanDiscovery,
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
                // Register a custom bean for injection points that are not handled by ConfigProducer
                Type injectedType = injectionPoint.getRequiredType();
                if (!isHandledByProducers(injectedType)) {
                    customBeanTypes.add(injectedType);
                }
            }
        }

        for (Type type : customBeanTypes) {
            if (type.kind() != Kind.ARRAY) {
                // Implicit converters are most likely used
                reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder(type.name().toString()).methods().build());
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
    void validateConfigInjectionPoints(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ConfigPropertyBuildItem> configProperties) {

        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
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

                Type injectedType = injectionPoint.getType();
                if (DotNames.OPTIONAL.equals(injectedType.name())
                        || DotNames.OPTIONAL_INT.equals(injectedType.name())
                        || DotNames.OPTIONAL_LONG.equals(injectedType.name())
                        || DotNames.OPTIONAL_DOUBLE.equals(injectedType.name())
                        || DotNames.INSTANCE.equals(injectedType.name())
                        || DotNames.PROVIDER.equals(injectedType.name())
                        || SUPPLIER_NAME.equals(injectedType.name())
                        || SR_CONFIG_VALUE_NAME.equals(injectedType.name())
                        || MP_CONFIG_VALUE_NAME.equals(injectedType.name())) {
                    // Never validate container objects
                    continue;
                }

                String propertyDefaultValue = null;
                if (defaultValue != null && (ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue.asString())
                        || !"".equals(defaultValue.asString()))) {
                    propertyDefaultValue = defaultValue.asString();
                }

                configProperties.produce(new ConfigPropertyBuildItem(propertyName, injectedType, propertyDefaultValue));
            }
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void validateConfigValues(ConfigRecorder recorder, List<ConfigPropertyBuildItem> configProperties,
            BeanContainerBuildItem beanContainer, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // IMPL NOTE: we do depend on BeanContainerBuildItem to make sure that the BeanDeploymentValidator finished its processing

        // the non-primitive types need to be registered for reflection since Class.forName is used at runtime to load the class
        for (ConfigPropertyBuildItem item : configProperties) {
            Type requiredType = item.getPropertyType();
            String propertyType = requiredType.name().toString();
            if (requiredType.kind() != Kind.PRIMITIVE) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(propertyType).build());
            }
        }

        Set<ConfigValidationMetadata> propertiesToValidate = new HashSet<>();
        for (ConfigPropertyBuildItem configProperty : configProperties) {
            String rawTypeName = configProperty.getPropertyType().name().toString();
            List<String> actualTypeArgumentNames = Collections.emptyList();
            if (configProperty.getPropertyType().kind() == Kind.PARAMETERIZED_TYPE) {
                List<Type> argumentTypes = configProperty.getPropertyType().asParameterizedType().arguments();
                actualTypeArgumentNames = new ArrayList<>(argumentTypes.size());
                for (Type argumentType : argumentTypes) {
                    actualTypeArgumentNames.add(argumentType.name().toString());
                    if (argumentType.kind() != Kind.PRIMITIVE) {
                        reflectiveClass.produce(ReflectiveClassBuildItem.builder(argumentType.name().toString())
                                .build());
                    }
                }

            }
            propertiesToValidate.add(new ConfigValidationMetadata(configProperty.getPropertyName(),
                    rawTypeName, actualTypeArgumentNames, configProperty.getDefaultValue()));
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
    void generateConfigProperties(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ConfigClassBuildItem> configClasses) {

        processConfigClasses(combinedIndex, generatedClasses, reflectiveClasses, configClasses, MP_CONFIG_PROPERTIES_NAME);
    }

    @BuildStep
    void registerConfigMappingsBean(
            BeanRegistrationPhaseBuildItem beanRegistration,
            List<ConfigClassBuildItem> configClasses,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BeanConfiguratorBuildItem> beanConfigurator) {

        if (configClasses.isEmpty()) {
            return;
        }

        Set<ConfigClassBuildItem> configMappings = new HashSet<>();

        // Add beans for all unremovable mappings
        for (ConfigClassBuildItem configClass : configClasses) {
            if (configClass.getConfigClass().isAnnotationPresent(Unremovable.class)) {
                configMappings.add(configClass);
            }
        }

        // Add beans for all injection points
        Map<Type, ConfigClassBuildItem> configMappingTypes = configClassesToTypesMap(configClasses, MAPPING);
        for (InjectionPointInfo injectionPoint : beanRegistration.getInjectionPoints()) {
            Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
            ConfigClassBuildItem configClass = configMappingTypes.get(type);
            if (configClass != null) {
                configMappings.add(configClass);
            }
        }

        // Generate the mappings beans
        for (ConfigClassBuildItem configClass : configMappings) {
            BeanConfigurator<Object> bean = beanRegistration.getContext()
                    .configure(configClass.getConfigClass())
                    .types(configClass.getTypes().toArray(new Type[] {}))
                    .creator(ConfigMappingCreator.class)
                    .addInjectionPoint(ClassType.create(DotNames.INJECTION_POINT))
                    .param("type", configClass.getConfigClass())
                    .param("prefix", configClass.getPrefix());

            if (configClass.getConfigClass().isAnnotationPresent(Unremovable.class)) {
                bean.unremovable();
            }

            beanConfigurator.produce(new BeanConfiguratorBuildItem(bean));
        }
    }

    @BuildStep
    void registerConfigPropertiesBean(
            BeanRegistrationPhaseBuildItem beanRegistration,
            List<ConfigClassBuildItem> configClasses,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BeanConfiguratorBuildItem> beanConfigurator) {

        if (configClasses.isEmpty()) {
            return;
        }

        Map<Type, ConfigClassBuildItem> configPropertiesTypes = configClassesToTypesMap(configClasses, PROPERTIES);
        Set<ConfigClassBuildItem> configProperties = new HashSet<>();
        for (InjectionPointInfo injectionPoint : beanRegistration.getInjectionPoints()) {
            AnnotationInstance instance = injectionPoint.getRequiredQualifier(MP_CONFIG_PROPERTIES_NAME);
            if (instance == null) {
                continue;
            }

            Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
            ConfigClassBuildItem configClass = configPropertiesTypes.get(type);
            if (configClass != null) {
                configProperties.add(configClass);
            }
        }

        for (ConfigClassBuildItem configClass : configProperties) {
            beanConfigurator.produce(new BeanConfiguratorBuildItem(
                    beanRegistration.getContext()
                            .configure(configClass.getConfigClass())
                            .types(configClass.getTypes().toArray(new Type[] {}))
                            .addQualifier(create(MP_CONFIG_PROPERTIES_NAME, null,
                                    new AnnotationValue[] {
                                            createStringValue("prefix", configClass.getPrefix())
                                    }))
                            .creator(ConfigMappingCreator.class)
                            .addInjectionPoint(ClassType.create(DotNames.INJECTION_POINT))
                            .param("type", configClass.getConfigClass())
                            .param("prefix", configClass.getPrefix())));
        }
    }

    @BuildStep
    void validateConfigMappingsInjectionPoints(
            ArcConfig arcConfig,
            ValidationPhaseBuildItem validationPhase,
            List<UnremovableBeanBuildItem> unremovableBeans,
            List<ConfigClassBuildItem> configClasses,
            BuildProducer<ConfigMappingBuildItem> configMappings) {

        if (configClasses.isEmpty()) {
            return;
        }

        Map<Type, ConfigClassBuildItem> configMappingTypes = configClassesToTypesMap(configClasses, MAPPING);
        Set<ConfigMappingBuildItem> toRegister = new HashSet<>();
        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
            ConfigClassBuildItem configClass = configMappingTypes.get(type);
            if (configClass != null) {
                AnnotationTarget target = injectionPoint.getTarget();
                AnnotationInstance mapping = null;
                if (target.kind().equals(FIELD)) {
                    mapping = target.asField().annotation(CONFIG_MAPPING_NAME);
                } else if (target.kind().equals(METHOD)) {
                    List<Type> parameters = target.asMethod().parameterTypes();
                    for (int i = 0; i < parameters.size(); i++) {
                        Type parameter = parameters.get(i);
                        if (parameter.name().equals(type.name())) {
                            Set<AnnotationInstance> parameterAnnotations = getParameterAnnotations(
                                    validationPhase.getBeanProcessor().getBeanDeployment(), target.asMethod(), i);
                            mapping = Annotations.find(parameterAnnotations, CONFIG_MAPPING_NAME);
                        }
                    }
                }

                AnnotationValue annotationPrefix = null;
                if (mapping != null) {
                    annotationPrefix = mapping.value("prefix");
                }

                String prefix = annotationPrefix != null ? annotationPrefix.asString() : configClass.getPrefix();
                toRegister.add(new ConfigMappingBuildItem(configClass.getConfigClass(), prefix));
            }
        }

        if (arcConfig.shouldEnableBeanRemoval()) {
            Set<String> unremovableClassNames = unremovableBeans.stream()
                    .map(UnremovableBeanBuildItem::getClassNames)
                    .flatMap(Collection::stream)
                    .collect(toSet());

            for (ConfigClassBuildItem configClass : configMappingTypes.values()) {
                if (configClass.getConfigClass().isAnnotationPresent(Unremovable.class)
                        || unremovableClassNames.contains(configClass.getName().toString())) {
                    toRegister.add(new ConfigMappingBuildItem(configClass.getConfigClass(), configClass.getPrefix()));
                }
            }
        }

        toRegister.forEach(configMappings::produce);
    }

    @BuildStep
    void validateConfigPropertiesInjectionPoints(
            ArcConfig arcConfig,
            ValidationPhaseBuildItem validationPhase,
            List<ConfigClassBuildItem> configClasses,
            BuildProducer<ConfigPropertiesBuildItem> configProperties) {

        if (configClasses.isEmpty()) {
            return;
        }

        Map<Type, ConfigClassBuildItem> configPropertiesTypes = configClassesToTypesMap(configClasses, PROPERTIES);
        Set<ConfigPropertiesBuildItem> toRegister = new HashSet<>();
        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            AnnotationInstance properties = injectionPoint.getRequiredQualifier(MP_CONFIG_PROPERTIES_NAME);
            if (properties != null) {
                Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
                ConfigClassBuildItem configClass = configPropertiesTypes.get(type);
                if (configClass != null) {
                    AnnotationValue annotationPrefix = properties.value("prefix");
                    String prefix = annotationPrefix != null && !annotationPrefix.asString().equals(UNCONFIGURED_PREFIX)
                            ? annotationPrefix.asString()
                            : configClass.getPrefix();
                    toRegister.add(new ConfigPropertiesBuildItem(configClass.getConfigClass(), prefix));
                }
            }
        }

        for (ConfigClassBuildItem configClass : configPropertiesTypes.values()) {
            if (!arcConfig.shouldEnableBeanRemoval()
                    || !validationPhase.getContext().beans().withBeanType(configClass.getConfigClass()).isEmpty()) {
                toRegister.add(new ConfigPropertiesBuildItem(configClass.getConfigClass(), configClass.getPrefix()));
            }
        }

        toRegister.forEach(configProperties::produce);
    }

    @BuildStep
    void warnStaticInitInjectionPoints(
            CombinedIndexBuildItem indexBuildItem,
            ValidationPhaseBuildItem validationPhase,
            List<ConfigClassBuildItem> configClasses,
            List<ConfigInjectionStaticInitBuildItem> configInjectionStaticInit,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefault) {

        // Add here annotated classes that are initialized during static init
        Set<DotName> declaringClassCandidates = configInjectionStaticInit.stream()
                .map(ConfigInjectionStaticInitBuildItem::getDeclaringCandidate).collect(toSet());

        Set<Type> configClassesTypes = configClasses.stream().map(ConfigClassBuildItem::getTypes).flatMap(Collection::stream)
                .collect(toSet());

        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            if (injectionPoint.getType().name().equals(DotNames.INSTANCE)) {
                continue;
            }

            Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
            DotName injectionTypeName = null;
            if (type.name().equals(MP_CONFIG) || type.name().equals(SR_CONFIG)) {
                injectionTypeName = type.name();
            } else if (injectionPoint.getRequiredQualifier(MP_CONFIG_PROPERTY_NAME) != null) {
                injectionTypeName = MP_CONFIG_PROPERTY_NAME;
            } else if (configClassesTypes.contains(type)) {
                injectionTypeName = type.name();
            }

            if (injectionTypeName != null) {
                AnnotationTarget target = injectionPoint.getTarget();
                if (FIELD.equals(target.kind())) {
                    FieldInfo field = target.asField();
                    ClassInfo declaringClass = field.declaringClass();
                    Map<DotName, List<AnnotationInstance>> annotationsMap = declaringClass.annotationsMap();
                    for (DotName declaringClassCandidate : declaringClassCandidates) {
                        List<AnnotationInstance> annotationInstances = annotationsMap.get(declaringClassCandidate);
                        if (annotationInstances != null && annotationInstances.size() == 1) {
                            AnnotationInstance annotationInstance = annotationInstances.get(0);
                            if (annotationInstance.target().equals(declaringClass)) {
                                LOGGER.warn("Directly injecting a " +
                                        injectionTypeName +
                                        " into a " +
                                        declaringClassCandidate +
                                        " may lead to unexpected results. To ensure proper results, please " +
                                        "change the type of the field to " +
                                        ParameterizedType.create(DotNames.INSTANCE, new Type[] { type }, null) +
                                        ". Offending field is '" +
                                        field.name() +
                                        "' of class '" +
                                        field.declaringClass() +
                                        "'");
                            }
                        }
                    }
                }
            }
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerConfigClasses(
            RecorderContext context,
            ConfigRecorder recorder,
            List<ConfigMappingBuildItem> configMappings,
            List<ConfigPropertiesBuildItem> configProperties) throws Exception {

        context.registerNonDefaultConstructor(
                ConfigClassWithPrefix.class.getDeclaredConstructor(Class.class, String.class),
                configClassWithPrefix -> Stream.of(configClassWithPrefix.getKlass(), configClassWithPrefix.getPrefix())
                        .collect(toList()));

        recorder.registerConfigProperties(
                configProperties.stream()
                        .map(p -> configClassWithPrefix(p.getConfigClass(), p.getPrefix()))
                        .collect(toSet()));
    }

    private static String getPropertyName(String name, ClassInfo declaringClass) {
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
                SR_CONFIG_VALUE_NAME.equals(type.name()) ||
                MP_CONFIG_VALUE_NAME.equals(type.name());
    }

    private static Map<Type, ConfigClassBuildItem> configClassesToTypesMap(List<ConfigClassBuildItem> configClasses,
            ConfigClassBuildItem.Kind kind) {
        Map<Type, ConfigClassBuildItem> configClassesTypes = new HashMap<>();
        for (ConfigClassBuildItem configClass : configClasses) {
            if (configClass.getKind().equals(kind)) {
                for (Type type : configClass.getTypes()) {
                    configClassesTypes.put(type, configClass);
                }
            }
        }
        return configClassesTypes;
    }
}
