package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.builditem.ConfigClassBuildItem.Type.MAPPING;
import static io.quarkus.deployment.builditem.ConfigClassBuildItem.Type.PROPERTIES;
import static java.util.Collections.emptySet;
import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;
import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;
import static org.jboss.jandex.AnnotationTarget.Kind.FIELD;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingMetadata;

public class ConfigMappingUtils {
    public static final DotName CONFIG_MAPPING_NAME = DotName.createSimple(ConfigMapping.class.getName());

    private ConfigMappingUtils() {
    }

    @BuildStep
    public static void generateConfigClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ConfigClassBuildItem> configClasses,
            DotName configAnnotation) {

        for (AnnotationInstance instance : combinedIndex.getIndex().getAnnotations(configAnnotation)) {
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

            // For implicit converters
            for (ClassInfo classInfo : mappingsInfo) {
                for (MethodInfo method : classInfo.methods()) {
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, method.returnType().name().toString()));
                }
            }

            configClasses.produce(toConfigClassBuildItem(instance, configClass, generatedClassesNames, prefix));
        }
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
        } else {
            return new ConfigClassBuildItem(configClass, generatedClasses, prefix, PROPERTIES);
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
}
