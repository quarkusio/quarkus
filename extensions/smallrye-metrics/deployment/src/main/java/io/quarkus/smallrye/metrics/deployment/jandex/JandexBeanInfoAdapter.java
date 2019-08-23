package io.quarkus.smallrye.metrics.deployment.jandex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames;
import io.smallrye.metrics.elementdesc.AnnotationInfo;
import io.smallrye.metrics.elementdesc.BeanInfo;
import io.smallrye.metrics.elementdesc.RawBeanInfo;
import io.smallrye.metrics.elementdesc.adapter.BeanInfoAdapter;

public class JandexBeanInfoAdapter implements BeanInfoAdapter<ClassInfo> {
    private static final DotName OBJECT = DotName.createSimple(Object.class.getName());
    private final IndexView indexView;

    public JandexBeanInfoAdapter(IndexView indexView) {
        this.indexView = indexView;
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
            List<AnnotationInfo> annotationsSuper = clazz.classAnnotations()
                    .stream()
                    .filter(SmallRyeMetricsDotNames::isMetricAnnotation)
                    .map(annotationInfoAdapter::convert)
                    .collect(Collectors.toList());
            annotations.addAll(annotationsSuper);
            clazz = indexView.getClassByName(clazz.superName());
        }

        return new RawBeanInfo(input.simpleName(),
                input.name().prefix().toString(),
                annotations,
                superClassInfo);
    }
}
