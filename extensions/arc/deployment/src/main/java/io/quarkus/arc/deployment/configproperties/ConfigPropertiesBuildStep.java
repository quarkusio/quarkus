package io.quarkus.arc.deployment.configproperties;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.DeploymentClassLoaderBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;

public class ConfigPropertiesBuildStep {

    @BuildStep
    void produceConfigPropertiesMetadata(CombinedIndexBuildItem combinedIndex, ArcConfig arcConfig,
            BuildProducer<ConfigPropertiesMetadataBuildItem> configPropertiesMetadataProducer) {
        for (AnnotationInstance annotation : combinedIndex.getIndex().getAnnotations(DotNames.CONFIG_PROPERTIES)) {
            configPropertiesMetadataProducer
                    .produce(
                            new ConfigPropertiesMetadataBuildItem(annotation, arcConfig.configPropertiesDefaultNamingStrategy));
        }
    }

    @BuildStep
    void setup(CombinedIndexBuildItem combinedIndex,
            ApplicationIndexBuildItem applicationIndex,
            List<ConfigPropertiesMetadataBuildItem> configPropertiesMetadataList,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigValues,
            BuildProducer<ConfigPropertyBuildItem> configProperties,
            DeploymentClassLoaderBuildItem deploymentClassLoader) {
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
                        classInfo.name(), generatedClassName);

            } else {
                /*
                 * In this case the producer method contains all the logic to instantiate the config class
                 * and call setters for value obtained from MP Config
                 */
                boolean needsValidation = ClassConfigPropertiesUtil.addProducerMethodForClassConfigProperties(
                        deploymentClassLoader.getClassLoader(), classInfo, producerClassCreator,
                        configPropertiesMetadata.getPrefix(), configPropertiesMetadata.getNamingStrategy(),
                        applicationIndex.getIndex(), configProperties);
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
