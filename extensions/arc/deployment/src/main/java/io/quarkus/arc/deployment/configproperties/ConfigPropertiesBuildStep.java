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

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;

public class ConfigPropertiesBuildStep {

    @BuildStep
    void produceConfigPropertiesMetadata(CombinedIndexBuildItem combinedIndex, ArcConfig arcConfig,
            BuildProducer<ConfigPropertiesMetadataBuildItem> configPropertiesMetadataProducer) {

        IndexView index = combinedIndex.getIndex();

        Map<DotName, ConfigProperties.NamingStrategy> namingStrategies = new HashMap<>();

        // handle @ConfigProperties
        for (AnnotationInstance instance : index.getAnnotations(DotNames.CONFIG_PROPERTIES)) {
            ClassInfo classInfo = instance.target().asClass();

            ConfigProperties.NamingStrategy namingStrategy = getNamingStrategy(arcConfig, instance.value("namingStrategy"));
            namingStrategies.put(classInfo.name(), namingStrategy);

            configPropertiesMetadataProducer
                    .produce(new ConfigPropertiesMetadataBuildItem(classInfo, getPrefix(instance), namingStrategy, false));
        }

        // handle @ConfigPrefix
        for (AnnotationInstance instance : index.getAnnotations(DotNames.CONFIG_PREFIX)) {
            ClassInfo classInfo;
            if (instance.target().kind() == AnnotationTarget.Kind.FIELD) {
                classInfo = index.getClassByName(instance.target().asField().type().name());
            } else if (instance.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                short position = instance.target().asMethodParameter().position();
                classInfo = index
                        .getClassByName(instance.target().asMethodParameter().method().parameters().get(position).name());
            } else {
                break;
            }

            // if the class was annotated with @ConfigProperties, use the strategy that was defined there, otherwise fallback to the default
            ConfigProperties.NamingStrategy namingStrategy = namingStrategies.getOrDefault(classInfo.name(),
                    arcConfig.configPropertiesDefaultNamingStrategy);

            configPropertiesMetadataProducer
                    .produce(new ConfigPropertiesMetadataBuildItem(classInfo, instance.value().asString(),
                            namingStrategy, true));
        }
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
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigValues,
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
        for (ConfigPropertiesMetadataBuildItem configPropertiesMetadata : configPropertiesMetadataList) {
            ClassInfo classInfo = configPropertiesMetadata.getClassInfo();

            if (Modifier.isInterface(classInfo.flags())) {
                /*
                 * In this case we need to generate an implementation of the interface that for each interface method
                 * simply pulls data from MP Config and returns it.
                 * The generated producer bean simply needs to return an instance of the generated class
                 */

                String generatedClassName = InterfaceConfigPropertiesUtil.generateImplementationForInterfaceConfigProperties(
                        classInfo, nonBeansClassOutput, combinedIndex.getIndex(), configPropertiesMetadata.getPrefix(),
                        configPropertiesMetadata.getNamingStrategy(), defaultConfigValues, configProperties);
                InterfaceConfigPropertiesUtil.addProducerMethodForInterfaceConfigProperties(producerClassCreator,
                        classInfo.name(), configPropertiesMetadata.getPrefix(), configPropertiesMetadata.isNeedsQualifier(),
                        generatedClassName);

            } else {
                /*
                 * In this case the producer method contains all the logic to instantiate the config class
                 * and call setters for value obtained from MP Config
                 */
                boolean needsValidation = ClassConfigPropertiesUtil.addProducerMethodForClassConfigProperties(
                        Thread.currentThread().getContextClassLoader(), classInfo, producerClassCreator,
                        configPropertiesMetadata.getPrefix(), configPropertiesMetadata.getNamingStrategy(),
                        configPropertiesMetadata.isNeedsQualifier(),
                        combinedIndex.getIndex(), configProperties);
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
