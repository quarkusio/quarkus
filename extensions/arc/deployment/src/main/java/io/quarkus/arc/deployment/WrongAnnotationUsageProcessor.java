package io.quarkus.arc.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;

public class WrongAnnotationUsageProcessor {

    @BuildStep
    void detect(ArcConfig config, ApplicationIndexBuildItem applicationIndex, CustomScopeAnnotationsBuildItem scopeAnnotations,
            TransformedAnnotationsBuildItem transformedAnnotations, BuildProducer<ValidationErrorBuildItem> validationErrors,
            InterceptorResolverBuildItem interceptorResolverBuildItem) {

        if (!config.detectWrongAnnotations) {
            return;
        }

        IndexView index = applicationIndex.getIndex();

        // Detect unsupported annotations
        List<UnsupportedAnnotation> unsupported = new ArrayList<>();

        String correctSingleton = "@jakarta.inject.Singleton";
        unsupported.add(new UnsupportedAnnotation("com.google.inject.Singleton", correctSingleton));
        unsupported.add(new UnsupportedAnnotation("jakarta.ejb.Singleton", correctSingleton));
        unsupported.add(new UnsupportedAnnotation("groovy.lang.Singleton", correctSingleton));
        unsupported.add(new UnsupportedAnnotation("javax.inject.Singleton", correctSingleton));

        String correctInject = "@jakarta.inject.Inject";
        unsupported.add(new UnsupportedAnnotation("javax.inject.Inject", correctInject));
        unsupported.add(new UnsupportedAnnotation("com.google.inject.Inject", correctInject));
        unsupported.add(new UnsupportedAnnotation("com.oracle.svm.core.annotate.Inject", correctInject));
        unsupported.add(new UnsupportedAnnotation("org.gradle.internal.impldep.javax.inject.Inject",
                correctInject));

        unsupported.add(new UnsupportedAnnotation("javax.annotation.PostConstruct", "@jakarta.annotation.PostConstruct"));
        unsupported.add(new UnsupportedAnnotation("javax.annotation.PreDestroy", "@jakarta.annotation.PreDestroy"));

        Map<AnnotationInstance, String> wrongUsages = new HashMap<>();

        for (UnsupportedAnnotation annotation : unsupported) {
            for (AnnotationInstance annotationInstance : index.getAnnotations(annotation.name)) {
                wrongUsages.put(annotationInstance, String.format("%s declared on %s, use %s instead",
                        annotationInstance.toString(false), getTargetInfo(annotationInstance), annotation.correctAnnotation));
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
                // Annotations declared on the class level, incl. the annotations added via transformers
                Collection<AnnotationInstance> classLevelAnnotations = transformedAnnotations.getAnnotations(clazz);
                if (scopeAnnotations.isScopeIn(classLevelAnnotations)) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new IllegalStateException(String.format(
                                    "The %s class %s has a scope annotation but it must be ignored per the CDI rules",
                                    clazz.nestingType().toString(), clazz.name().toString()))));
                } else if (Annotations.containsAny(classLevelAnnotations,
                        interceptorResolverBuildItem.getInterceptorBindings())) {
                    // detect interceptor bindings declared at nested class level
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new IllegalStateException(String.format(
                                    "The %s class %s declares an interceptor binding but it must be ignored per CDI rules",
                                    clazz.nestingType().toString(), clazz.name().toString()))));
                }

                // iterate over methods and verify those
                // note that since JDK 16, you can have static method inside inner non-static class
                for (MethodInfo methodInfo : clazz.methods()) {
                    // annotations declared on method level, incl. the annotations added via transformers
                    Collection<AnnotationInstance> methodAnnotations = transformedAnnotations.getAnnotations(methodInfo);
                    if (methodAnnotations.isEmpty()) {
                        continue;
                    }
                    if (Annotations.contains(methodAnnotations, DotNames.OBSERVES)
                            || Annotations.contains(methodAnnotations, DotNames.OBSERVES_ASYNC)) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException(String.format(
                                        "The method %s in the %s class %s declares an observer method but it must be ignored per the CDI rules",
                                        methodInfo.name(), clazz.nestingType().toString(), clazz.name().toString()))));
                    } else if (Annotations.contains(methodAnnotations, DotNames.PRODUCES)) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException(String.format(
                                        "The method %s in the %s class %s declares a producer but it must be ignored per the CDI rules",
                                        methodInfo.name(), clazz.nestingType().toString(), clazz.name().toString()))));
                    } else if (!Modifier.isStatic(methodInfo.flags()) && Annotations.containsAny(methodAnnotations,
                            interceptorResolverBuildItem.getInterceptorBindings())) {
                        // detect interceptor bindings declared at nested class methods
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException(String.format(
                                        "The method %s in the %s class %s declares an interceptor binding but it must be ignored per CDI rules",
                                        methodInfo.name(), clazz.nestingType().toString(), clazz.name().toString()))));
                    }

                }

                // iterate over all fields, check for incorrect producer declarations
                for (FieldInfo fieldInfo : clazz.fields()) {
                    // annotations declared on field level, incl. the annotations added via transformers
                    Collection<AnnotationInstance> fieldAnnotations = transformedAnnotations.getAnnotations(fieldInfo);
                    if (fieldAnnotations.isEmpty()) {
                        continue;
                    }
                    if (Annotations.contains(fieldAnnotations, DotNames.PRODUCES)) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException(String.format(
                                        "The field %s in the %s class %s declares a producer but it must be ignored per the CDI rules",
                                        fieldInfo.name(), clazz.nestingType().toString(), clazz.name().toString()))));
                    }
                }
            }
        }
    }

    private static class UnsupportedAnnotation {

        final DotName name;
        final String correctAnnotation;

        UnsupportedAnnotation(String name, String correctAnnotation) {
            this.name = DotName.createSimple(name);
            this.correctAnnotation = correctAnnotation;
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
