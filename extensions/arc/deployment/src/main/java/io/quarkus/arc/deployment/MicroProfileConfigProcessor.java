package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static java.util.stream.Collectors.toCollection;
import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;
import static org.jboss.jandex.AnnotationInstance.create;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER;
import static org.jboss.jandex.AnnotationValue.createStringValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.arc.runtime.ConfigPropertiesCreator;
import io.quarkus.arc.runtime.ConfigPropertyCreator;
import io.quarkus.arc.runtime.MicroProfileConfigRecorder;
import io.quarkus.arc.runtime.MicroProfileConfigRecorder.ConfigValidationMetadata;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigPropertiesBuildItem;
import io.quarkus.deployment.builditem.ConfigPropertiesRegistrarBuildItem;
import io.quarkus.deployment.builditem.GeneratedConfigClassBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.smallrye.config.inject.ConfigProducer;

/**
 * MicroProfile Config related build steps.
 */
public class MicroProfileConfigProcessor {

    static final DotName MP_CONFIG_PROPERTY = DotName.createSimple(ConfigProperty.class.getName());
    private static final DotName MP_CONFIG_PROPERTIES = DotName.createSimple(ConfigProperties.class.getName());
    private static final DotName MP_CONFIG_VALUE = DotName.createSimple(ConfigValue.class.getName());

    private static final DotName SR_CONFIG_VALUE = DotName.createSimple(io.smallrye.config.ConfigValue.class.getName());

    private static final DotName MAP = DotName.createSimple(Map.class.getName());
    private static final DotName SET = DotName.createSimple(Set.class.getName());
    private static final DotName LIST = DotName.createSimple(List.class.getName());
    private static final DotName SUPPLIER = DotName.createSimple(Supplier.class.getName());

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

            AnnotationInstance configProperty = injectionPoint.getRequiredQualifier(MP_CONFIG_PROPERTY);
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
                        .produce(ReflectiveClassBuildItem.builder(type.name().toString()).methods()
                                .reason(getClass().getName() + " Custom config bean")
                                .build());
            }
            DotName implClazz = type.kind() == Kind.ARRAY ? DotName.createSimple(ConfigPropertyCreator.class.getName())
                    : type.name();
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(implClazz)
                    .creator(ConfigPropertyCreator.class)
                    .providerType(type)
                    .types(type)
                    .addQualifier(MP_CONFIG_PROPERTY)
                    .param("requiredType", type.name().toString()).done());
        }
    }

    @BuildStep
    void configPropertyInjectionPoints(
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<ConfigPropertyBuildItem> configProperties) {

        // @Observes @Initialized(ApplicationScoped.class) requires validation at static init
        Set<MethodInfo> observerMethods = new HashSet<>();
        for (ObserverInfo observer : validationPhase.getBeanProcessor().getBeanDeployment().getObservers()) {
            if (observer.isSynthetic()) {
                continue;
            }
            AnnotationInstance instance = Annotations.getParameterAnnotation(observer.getObserverMethod(),
                    DotNames.INITIALIZED);
            if (instance != null && instance.value().asClass().name().equals(BuiltinScope.APPLICATION.getName())) {
                observerMethods.add(observer.getObserverMethod());
            }
        }

        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            if (injectionPoint.hasDefaultedQualifier()) {
                // Defaulted qualifier means no @ConfigProperty
                continue;
            }

            AnnotationInstance configProperty = injectionPoint.getRequiredQualifier(MP_CONFIG_PROPERTY);
            if (configProperty != null) {
                AnnotationValue nameValue = configProperty.value("name");
                AnnotationValue defaultValue = configProperty.value("defaultValue");
                String propertyName;
                if (nameValue != null) {
                    propertyName = nameValue.asString();
                } else {
                    // org.acme.Foo.config
                    if (injectionPoint.isField()) {
                        FieldInfo field = injectionPoint.getAnnotationTarget().asField();
                        propertyName = getPropertyName(field.name(), field.declaringClass());
                    } else if (injectionPoint.isParam()) {
                        MethodParameterInfo methodParameterInfo = injectionPoint.getAnnotationTarget().asMethodParameter();
                        propertyName = getPropertyName(methodParameterInfo.name(),
                                methodParameterInfo.method().declaringClass());
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
                        || SUPPLIER.equals(injectedType.name())
                        || SR_CONFIG_VALUE.equals(injectedType.name())
                        || MP_CONFIG_VALUE.equals(injectedType.name())) {
                    // Never validate container objects
                    continue;
                }

                String propertyDefaultValue = null;
                if (defaultValue != null && (ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue.asString())
                        || !"".equals(defaultValue.asString()))) {
                    propertyDefaultValue = defaultValue.asString();
                }

                if (injectionPoint.getAnnotationTarget().kind().equals(METHOD_PARAMETER)
                        && observerMethods.contains(injectionPoint.getAnnotationTarget().asMethodParameter().method())) {
                    configProperties
                            .produce(ConfigPropertyBuildItem.staticInit(propertyName, injectedType, propertyDefaultValue));
                }

                configProperties.produce(ConfigPropertyBuildItem.runtimeInit(propertyName, injectedType, propertyDefaultValue));
            }
        }
    }

    @BuildStep
    @Record(STATIC_INIT)
    void validateStaticInitConfigProperty(
            MicroProfileConfigRecorder recorder,
            List<ConfigPropertyBuildItem> configProperties,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        recorder.validateConfigProperties(
                configProperties.stream()
                        .filter(ConfigPropertyBuildItem::isStaticInit)
                        .map(p -> configPropertyToConfigValidation(p, reflectiveClass))
                        .collect(toCollection(LinkedHashSet::new)));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void validateRuntimeConfigProperty(
            MicroProfileConfigRecorder recorder,
            List<ConfigPropertyBuildItem> configProperties,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        recorder.validateConfigProperties(
                configProperties.stream()
                        .filter(ConfigPropertyBuildItem::isRuntimeInit)
                        .map(p -> configPropertyToConfigValidation(p, reflectiveClass))
                        .collect(toCollection(LinkedHashSet::new)));
    }

    /**
     * We veto the {@link org.eclipse.microprofile.config.inject.ConfigProperties} class bean, because we are going to
     * replace it by a custom synthetic bean.
     *
     * @see #registerConfigPropertiesBeans(java.util.List, io.quarkus.deployment.annotations.BuildProducer)
     */
    @BuildStep
    AnnotationsTransformerBuildItem vetoMPConfigProperties() {
        return new AnnotationsTransformerBuildItem(
                AnnotationTransformation.forClasses()
                        .whenAnyMatch(MP_CONFIG_PROPERTIES)
                        .transform(context -> context.add(Vetoed.class)));
    }

    @BuildStep
    void discoverConfigProperties(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ConfigPropertiesBuildItem> configProperties) {

        for (AnnotationInstance instance : combinedIndex.getIndex().getAnnotations(MP_CONFIG_PROPERTIES)) {
            AnnotationTarget target = instance.target();
            if (!target.kind().equals(CLASS)) {
                continue;
            }

            AnnotationValue annotationPrefix = instance.value("prefix");
            ClassInfo configClass = target.asClass();
            String prefix = annotationPrefix != null && !annotationPrefix.asString().equals(UNCONFIGURED_PREFIX)
                    ? annotationPrefix.asString()
                    : "";
            configProperties.produce(new ConfigPropertiesBuildItem(configClass, prefix, combinedIndex.getIndex()));
        }
    }

    @BuildStep
    void generateConfigProperties(
            List<ConfigPropertiesBuildItem> configProperties,
            BuildProducer<GeneratedConfigClassBuildItem> configClasses) {

        for (ConfigPropertiesBuildItem properties : configProperties) {
            configClasses.produce(GeneratedConfigClassBuildItem.of(properties.getConfigClass().name()));
        }
    }

    @BuildStep
    void registerConfigPropertiesBeans(
            List<ConfigPropertiesBuildItem> configProperties,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {

        for (ConfigPropertiesBuildItem configClass : configProperties) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator bean = SyntheticBeanBuildItem
                    .configure(configClass.getConfigClass().name())
                    .types(configClass.getTypes().toArray(new Type[] {}))
                    .addQualifier(create(MP_CONFIG_PROPERTIES, null,
                            new AnnotationValue[] {
                                    createStringValue("prefix", configClass.getPrefix())
                            }))
                    .addInjectionPoint(ClassType.create(DotNames.INJECTION_POINT))
                    .creator(ConfigPropertiesCreator.class)
                    .param("type", configClass.getConfigClass())
                    .param("prefix", configClass.getPrefix());

            if (configClass.getConfigClass().hasDeclaredAnnotation(DotNames.UNREMOVABLE)) {
                bean.unremovable();
            }

            syntheticBean.produce(bean.done());
        }
    }

    /**
     * Looks for prefixes overrides in injection points to provide the required registration in
     * {@link io.smallrye.config.SmallRyeConfig}.
     * <p>
     * It uses the {@link io.quarkus.arc.deployment.ValidationPhaseBuildItem} to determine which injection points
     * are actually used (the list does not contain the unused ones), to register only the required config classes
     * with {@link io.smallrye.config.SmallRyeConfig}. Otherwise, it may fail with missing configuration, even if the
     * injection point is unused.
     */
    @BuildStep
    void activeConfigPropertiesInjectionPoints(
            ArcConfig arcConfig,
            ValidationPhaseBuildItem validationPhase,
            List<ConfigPropertiesBuildItem> configProperties,
            BuildProducer<ConfigPropertiesRegistrarBuildItem> configPropertiesRegistrar) {

        Map<String, Set<String>> toRegister = new HashMap<>();
        for (InjectionPointInfo injectionPoint : validationPhase.getContext().getInjectionPoints()) {
            AnnotationInstance properties = injectionPoint.getRequiredQualifier(MP_CONFIG_PROPERTIES);
            if (properties != null) {
                Type type = Type.create(injectionPoint.getRequiredType().name(), Type.Kind.CLASS);
                for (ConfigPropertiesBuildItem configClass : configProperties) {
                    if (configClass.getTypes().contains(type)) {
                        AnnotationValue annotationPrefix = properties.value("prefix");
                        String prefix = annotationPrefix != null ? annotationPrefix.asString() : configClass.getPrefix();
                        toRegister.putIfAbsent(configClass.getConfigClassName(), new HashSet<>());
                        toRegister.get(configClass.getConfigClassName()).add(prefix);
                    }
                }
            }
        }

        for (ConfigPropertiesBuildItem configClass : configProperties) {
            if (!arcConfig.shouldEnableBeanRemoval()
                    || configClass.getConfigClass().hasDeclaredAnnotation(DotNames.UNREMOVABLE)) {
                toRegister.putIfAbsent(configClass.getConfigClassName(), new HashSet<>());
                toRegister.get(configClass.getConfigClassName()).add(configClass.getPrefix());
            }
        }

        configPropertiesRegistrar.produce(new ConfigPropertiesRegistrarBuildItem(toRegister));
    }

    /**
     * Registers the {@link org.eclipse.microprofile.config.inject.ConfigProperties} beans after
     * the creation of {@link io.smallrye.config.SmallRyeConfig}. It should be possible to register these in the config
     * builder during build time, but unfortunately the MP Config TCK requires to throw a
     * {@link jakarta.enterprise.inject.spi.DeploymentException} when a
     * {@link org.eclipse.microprofile.config.inject.ConfigProperties} bean cannot be initialized (due to missing
     * configuration). When {@link io.smallrye.config.SmallRyeConfig} cannot map a config class, it throws a
     * {@link io.smallrye.config.ConfigValidationException}. The recorder catches this exception and throws the
     * expected {@link jakarta.enterprise.inject.spi.DeploymentException}.
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    void registerConfigClasses(
            MicroProfileConfigRecorder recorder,
            Optional<ConfigPropertiesRegistrarBuildItem> configPropertiesRegistrar,
            BuildProducer<ServiceStartBuildItem> serviceStart) {

        if (configPropertiesRegistrar.isEmpty()) {
            return;
        }

        recorder.registerConfigProperties(configPropertiesRegistrar.get().getConfigProperties());

        // Ensure that @ConfigProperties are registered before Startup events
        serviceStart.produce(new ServiceStartBuildItem("microprofile-config-properties"));
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
                MAP.equals(type.name()) ||
                SET.equals(type.name()) ||
                LIST.equals(type.name()) ||
                DotNames.LONG.equals(type.name()) ||
                DotNames.FLOAT.equals(type.name()) ||
                DotNames.INTEGER.equals(type.name()) ||
                DotNames.BOOLEAN.equals(type.name()) ||
                DotNames.DOUBLE.equals(type.name()) ||
                DotNames.SHORT.equals(type.name()) ||
                DotNames.BYTE.equals(type.name()) ||
                DotNames.CHARACTER.equals(type.name()) ||
                SUPPLIER.equals(type.name()) ||
                SR_CONFIG_VALUE.equals(type.name()) ||
                MP_CONFIG_VALUE.equals(type.name());
    }

    private static ConfigValidationMetadata configPropertyToConfigValidation(ConfigPropertyBuildItem configProperty,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        String typeName = configProperty.getPropertyType().name().toString();
        List<String> typeArgumentNames = Collections.emptyList();

        if (configProperty.getPropertyType().kind() != Kind.PRIMITIVE) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(typeName)
                    .reason(MicroProfileConfigProcessor.class.getSimpleName() + " Configuration property")
                    .build());
        }

        if (configProperty.getPropertyType().kind() == Kind.PARAMETERIZED_TYPE) {
            List<Type> argumentTypes = configProperty.getPropertyType().asParameterizedType().arguments();
            typeArgumentNames = new ArrayList<>(argumentTypes.size());
            final List<String> forReflection = new ArrayList<>(argumentTypes.size());
            final var reason = MicroProfileConfigProcessor.class.getSimpleName() + " Configuration property's " + typeName
                    + " parameter";
            for (Type argumentType : argumentTypes) {
                final var argTypeClassName = argumentType.name().toString();
                typeArgumentNames.add(argTypeClassName);
                if (argumentType.kind() != Kind.PRIMITIVE) {
                    forReflection.add(argTypeClassName);
                }
            }
            if (!forReflection.isEmpty()) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(forReflection)
                        .reason(reason)
                        .build());
            }
        }

        return new ConfigValidationMetadata(configProperty.getPropertyName(), typeName, typeArgumentNames,
                configProperty.getDefaultValue());
    }
}
