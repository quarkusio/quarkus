package io.quarkus.arc.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;

public class WrongAnnotationsProcessor {

    @BuildStep
    void detect(ArcConfig config, ApplicationIndexBuildItem applicationIndex,
            BuildProducer<ValidationErrorBuildItem> validationError) {

        if (!config.detectWrongAnnotations) {
            return;
        }

        IndexView index = applicationIndex.getIndex();
        List<WrongAnnotation> wrongAnnotations = new ArrayList<>();
        Function<AnnotationInstance, String> singletonFun = new Function<AnnotationInstance, String>() {

            @Override
            public String apply(AnnotationInstance annotationInstance) {
                return String.format("%s declared on %s, use @javax.inject.Singleton instead",
                        annotationInstance.toString(false), getTargetInfo(annotationInstance));
            }
        };
        wrongAnnotations.add(new WrongAnnotation(DotName.createSimple("com.google.inject.Singleton"), singletonFun));
        wrongAnnotations.add(new WrongAnnotation(DotName.createSimple("javax.ejb.Singleton"), singletonFun));
        wrongAnnotations.add(new WrongAnnotation(DotName.createSimple("groovy.lang.Singleton"), singletonFun));
        wrongAnnotations.add(new WrongAnnotation(DotName.createSimple("jakarta.ejb.Singleton"), singletonFun));

        Map<AnnotationInstance, String> wrongUsages = new HashMap<>();

        for (WrongAnnotation wrongAnnotation : wrongAnnotations) {
            for (AnnotationInstance annotationInstance : index.getAnnotations(wrongAnnotation.name)) {
                wrongUsages.put(annotationInstance, wrongAnnotation.messageFun.apply(annotationInstance));
            }
        }

        if (!wrongUsages.isEmpty()) {
            for (Entry<AnnotationInstance, String> entry : wrongUsages.entrySet()) {
                validationError.produce(new ValidationErrorBuildItem(
                        new IllegalStateException(entry.getValue())));
            }
        }
    }

    private static class WrongAnnotation {

        final DotName name;
        final Function<AnnotationInstance, String> messageFun;

        WrongAnnotation(DotName name, Function<AnnotationInstance, String> messageFun) {
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
