package io.quarkus.micrometer.deployment.binder.mpmetrics;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;

/**
 * Create beans to handle MP Metrics API annotations. It is ok to import and use classes that reference MP Metrics
 * classes.
 */
public class AnnotationHandler {
    private static final Logger log = Logger.getLogger(AnnotationHandler.class);

    static AnnotationsTransformerBuildItem transformAnnotations(final IndexView index, DotName meterAnnotation) {
        return transformAnnotations(index, meterAnnotation, meterAnnotation);
    }

    static AnnotationsTransformerBuildItem transformAnnotations(final IndexView index, DotName sourceAnnotationName,
            DotName targetAnnotationName) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public void transform(TransformationContext ctx) {
                final Collection<AnnotationInstance> annotations = ctx.getAnnotations();
                AnnotationInstance annotation = Annotations.find(annotations, sourceAnnotationName);
                if (annotation == null) {
                    return;
                }
                AnnotationTarget target = ctx.getTarget();

                ClassInfo classInfo = null;
                MethodInfo methodInfo = null;
                FieldInfo fieldInfo = null;
                if (ctx.isMethod()) {
                    methodInfo = target.asMethod();
                    classInfo = methodInfo.declaringClass();
                } else if (ctx.isField()) {
                    fieldInfo = target.asField();
                    classInfo = fieldInfo.declaringClass();
                } else if (ctx.isClass()) {
                    classInfo = target.asClass();
                    // skip @Interceptor
                    if (target.asClass().declaredAnnotation(DotNames.INTERCEPTOR) != null) {
                        return;
                    }
                }

                // Remove the @Counted annotation when both @Counted & @Timed/SimplyTimed
                // Ignore @Metric with @Produces
                if (removeCountedWhenTimed(sourceAnnotationName, target, classInfo, methodInfo)
                        || removeMetricWhenProduces(sourceAnnotationName, target, methodInfo, fieldInfo)) {
                    ctx.transform().remove(x -> x == annotation).done();
                    return;
                }

                // Make sure all attributes exist:
                MetricAnnotationInfo annotationInfo = new MetricAnnotationInfo(annotation, index, classInfo, methodInfo,
                        fieldInfo);

                // preserve the original annotation target, `ctx.getTarget()` is different in case of method parameters
                AnnotationInstance newAnnotation = AnnotationInstance.create(targetAnnotationName, annotation.target(),
                        annotationInfo.getAnnotationValues());

                // Remove the existing annotation, and add a new one with all the fields
                ctx.transform().remove(x -> x == annotation).add(newAnnotation).done();
            }
        });
    }

    static boolean removeCountedWhenTimed(DotName sourceAnnotation, AnnotationTarget target, ClassInfo classInfo,
            MethodInfo methodInfo) {
        if (MetricDotNames.COUNTED_ANNOTATION.equals(sourceAnnotation)) {
            if (methodInfo == null) {
                if (!Annotations.contains(classInfo.declaredAnnotations(), MetricDotNames.TIMED_ANNOTATION)
                        && !Annotations.contains(classInfo.declaredAnnotations(),
                                MetricDotNames.SIMPLY_TIMED_ANNOTATION)) {
                    return false;
                }
                log.warnf(
                        "Bean %s is both counted and timed. The @Counted annotation "
                                + "will be suppressed in favor of the count emitted by the timer.",
                        classInfo.name().toString());
                return true;
            } else {
                if (!methodInfo.hasAnnotation(MetricDotNames.SIMPLY_TIMED_ANNOTATION)
                        && !methodInfo.hasAnnotation(MetricDotNames.TIMED_ANNOTATION)) {
                    return false;
                }
                log.warnf(
                        "Method %s of bean %s is both counted and timed. The @Counted "
                                + "annotation will be suppressed in favor of the count emitted by the timer.",
                        methodInfo.name(), classInfo.name().toString());
                return true;
            }
        }
        return false;
    }

    private static boolean removeMetricWhenProduces(DotName sourceAnnotation, AnnotationTarget target,
            MethodInfo methodInfo, FieldInfo fieldInfo) {
        if (MetricDotNames.METRIC_ANNOTATION.equals(sourceAnnotation)) {
            if ((methodInfo != null && !methodInfo.hasAnnotation(DotNames.PRODUCES))
                    || (fieldInfo != null && !fieldInfo.hasAnnotation(DotNames.PRODUCES))) {
                return false;
            }
            log.errorf(
                    "A declared bean uses the @Metric annotation with a @Producer "
                            + "field or method, which is not compatible with micrometer support. "
                            + "The annotation target will be ignored. (%s - %s)",
                    target, System.identityHashCode(target));
            return true;
        }
        return false;
    }

}
