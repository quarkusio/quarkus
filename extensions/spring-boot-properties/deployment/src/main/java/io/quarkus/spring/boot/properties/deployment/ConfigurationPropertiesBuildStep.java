package io.quarkus.spring.boot.properties.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
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
import io.quarkus.spring.boot.properties.deployment.InterfaceConfigurationPropertiesUtil.GeneratedClass;

public class ConfigurationPropertiesBuildStep {

    @BuildStep
    void setup(CombinedIndexBuildItem combinedIndex,
            List<ConfigurationPropertiesMetadataBuildItem> configPropertiesMetadataList,
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
                .className(ConfigurationPropertiesUtil.PACKAGE_TO_PLACE_GENERATED_CLASSES + ".ConfigPropertiesProducer")
                .build();
        producerClassCreator.addAnnotation(Singleton.class);

        Set<DotName> configClassesThatNeedValidation = new HashSet<>(configPropertiesMetadataList.size());
        IndexView index = combinedIndex.getIndex();
        YamlListObjectHandler yamlListObjectHandler = new YamlListObjectHandler(nonBeansClassOutput, index, reflectiveClasses);
        ClassConfigurationPropertiesUtil classConfigPropertiesUtil = new ClassConfigurationPropertiesUtil(index,
                yamlListObjectHandler, producerClassCreator, capabilities, reflectiveClasses, reflectiveMethods,
                configProperties);
        InterfaceConfigurationPropertiesUtil interfaceConfigPropertiesUtil = new InterfaceConfigurationPropertiesUtil(index,
                yamlListObjectHandler, nonBeansClassOutput, producerClassCreator, capabilities, defaultConfigValues,
                configProperties, reflectiveClasses);
        for (ConfigurationPropertiesMetadataBuildItem configPropertiesMetadata : configPropertiesMetadataList) {
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
                            configPropertiesMetadata.getPrefix(), entry.getValue());
                }
            } else {
                /*
                 * In this case the producer method contains all the logic to instantiate the config class
                 * and call setters for value obtained from MP Config
                 */
                boolean needsValidation = classConfigPropertiesUtil.addProducerMethodForClassConfigProperties(
                        Thread.currentThread().getContextClassLoader(), classInfo,
                        configPropertiesMetadata.getPrefix(), configPropertiesMetadata.getNamingStrategy(),
                        configPropertiesMetadata.isFailOnMismatchingMember(),
                        configPropertiesMetadata.getInstanceFactory());
                if (needsValidation) {
                    configClassesThatNeedValidation.add(classInfo.name());
                }
            }
        }

        producerClassCreator.close();

        if (!configClassesThatNeedValidation.isEmpty()) {
            ClassConfigurationPropertiesUtil.generateStartupObserverThatInjectsConfigClass(beansClassOutput,
                    configClassesThatNeedValidation);
        }
    }
}
