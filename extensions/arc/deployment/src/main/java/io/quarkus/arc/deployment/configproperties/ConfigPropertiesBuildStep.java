package io.quarkus.arc.deployment.configproperties;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.config.ConfigProperties;
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
    void setup(CombinedIndexBuildItem combinedIndex,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> defaultConfigValues,
            BuildProducer<ConfigPropertyBuildItem> configProperties,
            DeploymentClassLoaderBuildItem deploymentClassLoader) {
        IndexView index = combinedIndex.getIndex();
        Collection<AnnotationInstance> instances = index.getAnnotations(DotNames.CONFIG_PROPERTIES);
        if (instances.isEmpty()) {
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

        Set<DotName> configClassesThatNeedValidation = new HashSet<>(instances.size());
        for (AnnotationInstance configPropertiesInstance : instances) {
            ClassInfo classInfo = configPropertiesInstance.target().asClass();

            String prefixStr = determinePrefix(configPropertiesInstance);
            if (Modifier.isInterface(classInfo.flags())) {
                /*
                 * In this case we need to generate an implementation of the interface that for each interface method
                 * simply pulls data from MP Config and returns it.
                 * The generated producer bean simply needs to return an instance of the generated class
                 */

                String generatedClassName = InterfaceConfigPropertiesUtil.generateImplementationForInterfaceConfigProperties(
                        classInfo, nonBeansClassOutput, index, prefixStr,
                        defaultConfigValues, configProperties);
                InterfaceConfigPropertiesUtil.addProducerMethodForInterfaceConfigProperties(producerClassCreator,
                        classInfo.name(), generatedClassName);

            } else {
                /*
                 * In this case the producer method contains all the logic to instantiate the config class
                 * and call setters for value obtained from MP Config
                 */
                boolean needsValidation = ClassConfigPropertiesUtil.addProducerMethodForClassConfigProperties(
                        deploymentClassLoader.getClassLoader(), classInfo, producerClassCreator, prefixStr,
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

    /**
     * Use the annotation value
     */
    private String determinePrefix(AnnotationInstance configPropertiesInstance) {
        String fromAnnotation = getPrefixFromAnnotation(configPropertiesInstance);
        if (fromAnnotation != null) {
            return fromAnnotation;
        }
        return getPrefixFromClassName(configPropertiesInstance.target().asClass().name());
    }

    private String getPrefixFromAnnotation(AnnotationInstance configPropertiesInstance) {
        AnnotationValue annotationValue = configPropertiesInstance.value("prefix");
        if (annotationValue == null) {
            return null;
        }
        String value = annotationValue.asString();
        if (ConfigProperties.UNSET_PREFIX.equals(value) || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private String getPrefixFromClassName(DotName className) {
        String simpleName = className.isInner() ? className.local() : className.withoutPackagePrefix();
        return join("-",
                withoutSuffix(lowerCase(camelHumpsIterator(simpleName)), "config", "configuration",
                        "properties", "props"));
    }

}
