package io.quarkus.arc.deployment.configproperties;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.BuildExclusionsBuildItem;
import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.configproperties.InterfaceConfigPropertiesUtil.GeneratedClass;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;

public class ConfigPropertiesBuildStep {

    @BuildStep
    void produceConfigPropertiesMetadata(CombinedIndexBuildItem combinedIndex, ArcConfig arcConfig,
            BuildExclusionsBuildItem exclusionsBuildItem,
            BuildProducer<ConfigPropertiesMetadataBuildItem> configPropertiesMetadataProducer) {

        IndexView index = combinedIndex.getIndex();

        Map<DotName, ConfigProperties.NamingStrategy> namingStrategies = new HashMap<>();
        Map<DotName, Boolean> failOnMismatchingMembers = new HashMap<>();

        // handle @ConfigProperties
        for (AnnotationInstance instance : index.getAnnotations(DotNames.CONFIG_PROPERTIES)) {
            final AnnotationTarget target = instance.target();
            if (exclusionsBuildItem.isExcluded(target)) {
                continue;
            }
            ClassInfo classInfo = target.asClass();
            ConfigProperties.NamingStrategy namingStrategy = getNamingStrategy(arcConfig, instance.value("namingStrategy"));
            namingStrategies.put(classInfo.name(), namingStrategy);

            boolean failOnMismatchingMember = isFailOnMissingMember(instance);
            failOnMismatchingMembers.put(classInfo.name(), failOnMismatchingMember);

            configPropertiesMetadataProducer
                    .produce(new ConfigPropertiesMetadataBuildItem(classInfo, getPrefix(instance), namingStrategy,
                            failOnMismatchingMember, false));
        }

        // handle @ConfigPrefix
        for (AnnotationInstance instance : index.getAnnotations(DotNames.CONFIG_PREFIX)) {
            ClassInfo classInfo;
            final AnnotationTarget target = instance.target();
            if (exclusionsBuildItem.isExcluded(target)) {
                continue;
            }
            if (target.kind() == AnnotationTarget.Kind.FIELD) {
                classInfo = index.getClassByName(target.asField().type().name());
            } else if (target.kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                final MethodParameterInfo parameter = target.asMethodParameter();
                short position = parameter.position();
                classInfo = index.getClassByName(parameter.method().parameters().get(position).name());
            } else {
                break;
            }

            // if the class was annotated with @ConfigProperties, use the strategy that was defined there, otherwise fallback to the default
            ConfigProperties.NamingStrategy namingStrategy = namingStrategies.getOrDefault(classInfo.name(),
                    arcConfig.configPropertiesDefaultNamingStrategy);

            configPropertiesMetadataProducer
                    .produce(new ConfigPropertiesMetadataBuildItem(classInfo, instance.value().asString(),
                            namingStrategy, failOnMismatchingMembers.getOrDefault(classInfo.name(),
                                    ConfigProperties.DEFAULT_FAIL_ON_MISMATCHING_MEMBER),
                            true));
        }
    }

    private boolean isFailOnMissingMember(AnnotationInstance instance) {
        AnnotationValue failOnMissingMemberValue = instance.value("failOnMismatchingMember");
        return failOnMissingMemberValue != null ? failOnMissingMemberValue.asBoolean()
                : ConfigProperties.DEFAULT_FAIL_ON_MISMATCHING_MEMBER;
    }

    private ConfigProperties.NamingStrategy getNamingStrategy(ArcConfig arcConfig, AnnotationValue namingStrategyValue) {
        return namingStrategyValue == null ? arcConfig.configPropertiesDefaultNamingStrategy
                : ConfigProperties.NamingStrategy.valueOf(namingStrategyValue.asEnum());
    }

    private String getPrefix(AnnotationInstance annotationInstance) {
        AnnotationValue value = annotationInstance.value("prefix");
        return value == null ? null : value.asString();
    }

    @BuildStep
    void setup(CombinedIndexBuildItem combinedIndex,
            List<ConfigPropertiesMetadataBuildItem> configPropertiesMetadataList,
            Capabilities capabilities,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigValues,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ConfigPropertyBuildItem> configProperties) {
        if (configPropertiesMetadataList.isEmpty()) {
            return;
        }

        ClassOutput beansClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        ClassOutput nonBeansClassOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);

        /*
         * We generate CDI producer bean containing one method for each of the @ConfigProperties
         * instances we encounter
         */

        ClassCreator producerClassCreator = ClassCreator.builder().classOutput(beansClassOutput)
                .className(ConfigPropertiesUtil.PACKAGE_TO_PLACE_GENERATED_CLASSES + ".ConfigPropertiesProducer")
                .build();
        producerClassCreator.addAnnotation(Singleton.class);

        Set<DotName> configClassesThatNeedValidation = new HashSet<>(configPropertiesMetadataList.size());
        IndexView index = combinedIndex.getIndex();
        YamlListObjectHandler yamlListObjectHandler = new YamlListObjectHandler(nonBeansClassOutput, index, reflectiveClasses);
        ClassConfigPropertiesUtil classConfigPropertiesUtil = new ClassConfigPropertiesUtil(index,
                yamlListObjectHandler, producerClassCreator, capabilities, reflectiveClasses, reflectiveMethods,
                configProperties);
        InterfaceConfigPropertiesUtil interfaceConfigPropertiesUtil = new InterfaceConfigPropertiesUtil(index,
                yamlListObjectHandler, nonBeansClassOutput, producerClassCreator, capabilities, defaultConfigValues,
                configProperties, reflectiveClasses);
        for (ConfigPropertiesMetadataBuildItem configPropertiesMetadata : configPropertiesMetadataList) {
            ClassInfo classInfo = configPropertiesMetadata.getClassInfo();

            if (Modifier.isInterface(classInfo.flags())) {
                /*
                 * In this case we need to generate an implementation of the interface that for each interface method
                 * simply pulls data from MP Config and returns it.
                 * The generated producer bean simply needs to return an instance of the generated class
                 */

                Map<DotName, GeneratedClass> interfaceToGeneratedClass = new HashMap<>();
                interfaceConfigPropertiesUtil.generateImplementationForInterfaceConfigProperties(
                        classInfo, configPropertiesMetadata.getPrefix(),
                        configPropertiesMetadata.getNamingStrategy(),
                        interfaceToGeneratedClass);
                for (Map.Entry<DotName, GeneratedClass> entry : interfaceToGeneratedClass.entrySet()) {
                    interfaceConfigPropertiesUtil.addProducerMethodForInterfaceConfigProperties(entry.getKey(),
                            configPropertiesMetadata.getPrefix(), configPropertiesMetadata.isNeedsQualifier(),
                            entry.getValue());
                }
            } else {
                /*
                 * In this case the producer method contains all the logic to instantiate the config class
                 * and call setters for value obtained from MP Config
                 */
                boolean needsValidation = classConfigPropertiesUtil.addProducerMethodForClassConfigProperties(
                        Thread.currentThread().getContextClassLoader(), classInfo,
                        configPropertiesMetadata.getPrefix(), configPropertiesMetadata.getNamingStrategy(),
                        configPropertiesMetadata.isFailOnMismatchingMember(), configPropertiesMetadata.isNeedsQualifier());
                if (needsValidation) {
                    configClassesThatNeedValidation.add(classInfo.name());
                }
            }
        }

        producerClassCreator.close();

        if (!configClassesThatNeedValidation.isEmpty()) {
            ClassConfigPropertiesUtil.generateStartupObserverThatInjectsConfigClass(beansClassOutput,
                    configClassesThatNeedValidation);
        }
    }
}
