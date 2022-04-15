package io.quarkus.arc.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class ReflectiveBeanClassesProcessor {

    @BuildStep
    void implicitReflectiveBeanClasses(BuildProducer<ReflectiveBeanClassBuildItem> reflectiveBeanClasses,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished) {
        DotName registerForReflection = DotName.createSimple(RegisterForReflection.class.getName());

        for (BeanInfo classBean : beanDiscoveryFinished.beanStream().classBeans()) {
            ClassInfo beanClass = classBean.getTarget().get().asClass();
            AnnotationInstance annotation = beanClass.classAnnotation(registerForReflection);
            if (annotation != null) {
                Type[] targets = annotation.value("targets") != null ? annotation.value("targets").asClassArray()
                        : new Type[] {};
                String[] classNames = annotation.value("classNames") != null ? annotation.value("classNames").asStringArray()
                        : new String[] {};
                if (targets.length == 0 && classNames.length == 0) {
                    reflectiveBeanClasses.produce(new ReflectiveBeanClassBuildItem(beanClass));
                }
            }
        }
    }

}
