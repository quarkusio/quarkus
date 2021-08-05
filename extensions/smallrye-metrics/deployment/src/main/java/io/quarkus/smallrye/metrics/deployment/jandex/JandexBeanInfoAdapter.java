package io.quarkus.smallrye.metrics.deployment.jandex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames;
import io.smallrye.metrics.elementdesc.AnnotationInfo;
import io.smallrye.metrics.elementdesc.BeanInfo;
import io.smallrye.metrics.elementdesc.RawBeanInfo;
import io.smallrye.metrics.elementdesc.adapter.BeanInfoAdapter;

public class JandexBeanInfoAdapter implements BeanInfoAdapter<ClassInfo> {
    private static final DotName OBJECT = DotName.createSimple(Object.class.getName());
    private final IndexView indexView;
    private final TransformedAnnotationsBuildItem transformedAnnotations;

    public JandexBeanInfoAdapter(IndexView indexView, TransformedAnnotationsBuildItem transformedAnnotations) {
        this.indexView = indexView;
        this.transformedAnnotations = transformedAnnotations;
    }

    @Override
    public BeanInfo convert(ClassInfo input) {
        BeanInfo superClassInfo = null;
        DotName superName = input.superName();
        if (superName != null && indexView.getClassByName(superName) != null && !superName.equals(OBJECT)) {
            superClassInfo = this.convert(indexView.getClassByName(superName));
        }

        JandexAnnotationInfoAdapter annotationInfoAdapter = new JandexAnnotationInfoAdapter(indexView);

        // add all class-level annotations, including inherited - SmallRye expects them here
        List<AnnotationInfo> annotations = new ArrayList<>();
        ClassInfo clazz = input;
        while (clazz != null && clazz.superName() != null) {
            List<AnnotationInfo> annotationsSuper = transformedAnnotations.getAnnotations(clazz)
                    .stream()
                    .filter(SmallRyeMetricsDotNames::isMetricAnnotation)
                    .map(annotationInfoAdapter::convert)
                    .collect(Collectors.toList());
            annotations.addAll(annotationsSuper);

            // a metric annotation can also be added through a CDI stereotype, so look into stereotypes
            List<AnnotationInfo> annotationsThroughStereotypes = transformedAnnotations.getAnnotations(clazz)
                    .stream()
                    .flatMap(a -> getMetricAnnotationsThroughStereotype(a, indexView))
                    .collect(Collectors.toList());
            annotations.addAll(annotationsThroughStereotypes);

            clazz = indexView.getClassByName(clazz.superName());
        }

        return new RawBeanInfo(input.simpleName(),
                input.name().prefix() == null ? "" : input.name().prefix().toString(),
                annotations,
                superClassInfo);
    }

    private Stream<AnnotationInfo> getMetricAnnotationsThroughStereotype(AnnotationInstance stereotypeInstance,
            IndexView indexView) {
        ClassInfo annotationType = indexView.getClassByName(stereotypeInstance.name());
        if (annotationType.classAnnotation(DotNames.STEREOTYPE) != null) {
            JandexAnnotationInfoAdapter adapter = new JandexAnnotationInfoAdapter(indexView);
            return transformedAnnotations.getAnnotations(annotationType)
                    .stream()
                    .filter(SmallRyeMetricsDotNames::isMetricAnnotation)
                    .map(adapter::convert);
        } else {
            return Stream.empty();
        }
    }
}
