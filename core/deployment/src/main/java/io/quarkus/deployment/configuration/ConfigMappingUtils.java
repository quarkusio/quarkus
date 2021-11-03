package io.quarkus.deployment.configuration;

import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
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

    public static void generateConfigClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ConfigClassBuildItem> configClasses,
            DotName configAnnotation) {

        for (AnnotationInstance instance : combinedIndex.getIndex().getAnnotations(configAnnotation)) {
            AnnotationTarget target = instance.target();
            AnnotationValue annotationPrefix = instance.value("prefix");

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

            configClasses.produce(new ConfigClassBuildItem(configClass, collectTypes(combinedIndex, configClass),
                    generatedClassesNames, prefix, getConfigClassType(instance)));
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

    private static ConfigClassBuildItem.Kind getConfigClassType(AnnotationInstance instance) {
        if (instance.name().equals(CONFIG_MAPPING_NAME)) {
            return ConfigClassBuildItem.Kind.MAPPING;
        } else {
            return ConfigClassBuildItem.Kind.PROPERTIES;
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

    private static Set<Type> collectTypes(CombinedIndexBuildItem combinedIndex, Class<?> configClass) {
        IndexView index = combinedIndex.getIndex();
        DotName configIfaceName = DotName.createSimple(configClass.getName());
        ClassInfo configIfaceInfo = index.getClassByName(configIfaceName);
        if ((configIfaceInfo == null) || configIfaceInfo.interfaceNames().isEmpty()) {
            return Collections.singleton(Type.create(configIfaceName, Type.Kind.CLASS));
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
