package io.quarkus.arc.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;

public class WrongAnnotationUsageProcessor {

    @BuildStep
    void detect(ArcConfig config, ApplicationIndexBuildItem applicationIndex, CustomScopeAnnotationsBuildItem scopeAnnotations,
            TransformedAnnotationsBuildItem transformedAnnotations, BuildProducer<ValidationErrorBuildItem> validationErrors) {

        if (!config.detectWrongAnnotations) {
            return;
        }

        IndexView index = applicationIndex.getIndex();

        // Detect unsupported annotations
        List<UnsupportedAnnotation> unsupported = new ArrayList<>();
        Function<AnnotationInstance, String> singletonFun = new Function<AnnotationInstance, String>() {

            @Override
            public String apply(AnnotationInstance annotationInstance) {
                return String.format("%s declared on %s, use @jakarta.inject.Singleton instead",
                        annotationInstance.toString(false), getTargetInfo(annotationInstance));
            }
        };
        unsupported.add(new UnsupportedAnnotation(DotName.createSimple("com.google.inject.Singleton"), singletonFun));
        unsupported.add(new UnsupportedAnnotation(DotName.createSimple("jakarta.ejb.Singleton"), singletonFun));
        unsupported.add(new UnsupportedAnnotation(DotName.createSimple("groovy.lang.Singleton"), singletonFun));
        unsupported.add(new UnsupportedAnnotation(DotName.createSimple("jakarta.ejb.Singleton"), singletonFun));

        Map<AnnotationInstance, String> wrongUsages = new HashMap<>();

        for (UnsupportedAnnotation annotation : unsupported) {
            for (AnnotationInstance annotationInstance : index.getAnnotations(annotation.name)) {
                wrongUsages.put(annotationInstance, annotation.messageFun.apply(annotationInstance));
            }
        }

        if (!wrongUsages.isEmpty()) {
            for (Entry<AnnotationInstance, String> entry : wrongUsages.entrySet()) {
                validationErrors.produce(new ValidationErrorBuildItem(
                        new IllegalStateException(entry.getValue())));
            }
        }

        // Detect local, inner classes annotated with a scope, observer or producer
        // Note that a scope annotation is typically defined as @Target({TYPE, METHOD, FIELD}) but it's not forbidden to use ElementType.TYPE_USE
        for (ClassInfo clazz : index.getKnownClasses()) {
            NestingType nestingType = clazz.nestingType();
            if (NestingType.ANONYMOUS == nestingType || NestingType.LOCAL == nestingType
                    || (NestingType.INNER == nestingType && !Modifier.isStatic(clazz.flags()))) {
                // Annotations declared on the class, incl. the annotations added via transformers
                Collection<AnnotationInstance> classAnnotations = transformedAnnotations.getAnnotations(clazz);
                if (classAnnotations.isEmpty() && clazz.annotationsMap().isEmpty()) {
                    continue;
                }
                if (scopeAnnotations.isScopeIn(classAnnotations)) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new IllegalStateException(String.format(
                                    "The %s class %s has a scope annotation but it must be ignored per the CDI rules",
                                    clazz.nestingType().toString(), clazz.name().toString()))));
                } else if (clazz.annotationsMap().containsKey(DotNames.OBSERVES)) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new IllegalStateException(String.format(
                                    "The %s class %s declares an observer method but it must be ignored per the CDI rules",
                                    clazz.nestingType().toString(), clazz.name().toString()))));
                } else if (clazz.annotationsMap().containsKey(DotNames.PRODUCES)) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new IllegalStateException(String.format(
                                    "The %s class %s declares a producer but it must be ignored per the CDI rules",
                                    clazz.nestingType().toString(), clazz.name().toString()))));
                }
            }
        }
    }

    private static class UnsupportedAnnotation {

        final DotName name;
        final Function<AnnotationInstance, String> messageFun;

        UnsupportedAnnotation(DotName name, Function<AnnotationInstance, String> messageFun) {
            this.name = name;
            this.messageFun = messageFun;
        }

    }

    private static String getTargetInfo(AnnotationInstance annotationInstance) {
        AnnotationTarget target = annotationInstance.target();
        switch (target.kind()) {
            case FIELD:
                return target.asField().declaringClass().toString() + "."
                        + target.asField().name();
            case METHOD:
                return target.asMethod().declaringClass().toString()
                        + "."
                        + target.asMethod().name() + "()";
            default:
                return target.toString();
        }
    }

}
