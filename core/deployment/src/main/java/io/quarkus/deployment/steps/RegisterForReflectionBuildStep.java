package io.quarkus.deployment.steps;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class RegisterForReflectionBuildStep {

    private static final Logger log = Logger.getLogger(RegisterForReflectionBuildStep.class);

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (AnnotationInstance i : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RegisterForReflection.class.getName()))) {

            boolean methods = getBooleanValue(i, "methods");
            boolean fields = getBooleanValue(i, "fields");
            boolean ignoreNested = getBooleanValue(i, "ignoreNested");
            boolean serialization = i.value("serialization") != null && i.value("serialization").asBoolean();

            AnnotationValue targetsValue = i.value("targets");
            AnnotationValue classNamesValue = i.value("classNames");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (targetsValue == null && classNamesValue == null) {
                ClassInfo classInfo = i.target().asClass();
                registerClass(classLoader, classInfo.name().toString(), methods, fields, ignoreNested, serialization,
                        reflectiveClass);
                continue;
            }

            if (targetsValue != null) {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    registerClass(classLoader, type.name().toString(), methods, fields, ignoreNested, serialization,
                            reflectiveClass);
                }
            }

            if (classNamesValue != null) {
                String[] classNames = classNamesValue.asStringArray();
                for (String className : classNames) {
                    registerClass(classLoader, className, methods, fields, ignoreNested, serialization, reflectiveClass);
                }
            }
        }
    }

    /**
     * BFS Recursive Method to register a class and it's inner classes for Reflection.
     */
    private void registerClass(ClassLoader classLoader, String className, boolean methods, boolean fields,
            boolean ignoreNested, boolean serialization, final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(serialization ? ReflectiveClassBuildItem.serializationClass(methods, fields, className)
                : new ReflectiveClassBuildItem(methods, fields, className));

        if (ignoreNested) {
            return;
        }

        try {
            Class<?>[] declaredClasses = classLoader.loadClass(className).getDeclaredClasses();
            for (Class<?> clazz : declaredClasses) {
                registerClass(classLoader, clazz.getName(), methods, fields, false, serialization, reflectiveClass);
            }
        } catch (ClassNotFoundException e) {
            log.warnf(e, "Failed to load Class %s", className);
        }
    }

    private static boolean getBooleanValue(AnnotationInstance i, String name) {
        return i.value(name) == null || i.value(name).asBoolean();
    }
}
