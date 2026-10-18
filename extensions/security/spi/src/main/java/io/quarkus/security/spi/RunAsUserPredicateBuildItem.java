package io.quarkus.security.spi;

import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * The {@link io.quarkus.security.identity.RunAsUser} annotation can only be used on the {@link AnnotationTarget}
 * matching a predicate registered with this build item. Using the annotation on any other {@link AnnotationTarget}
 * results in validation failure that prevents unsupported scenarios.
 */
public final class RunAsUserPredicateBuildItem extends MultiBuildItem {

    private final Predicate<AnnotationTarget> annotationTargetPredicate;

    private RunAsUserPredicateBuildItem(Predicate<AnnotationTarget> annotationTargetPredicate) {
        this.annotationTargetPredicate = annotationTargetPredicate;
    }

    public static Predicate<AnnotationTarget> get(List<RunAsUserPredicateBuildItem> items) {
        return at -> items.stream().map(i -> i.annotationTargetPredicate).anyMatch(p -> p.test(at));
    }

    public static RunAsUserPredicateBuildItem ofAnnotation(Class<? extends Annotation> annotationClass) {
        return new RunAsUserPredicateBuildItem(at -> at.kind() == METHOD && at.hasAnnotation(annotationClass));
    }
}
