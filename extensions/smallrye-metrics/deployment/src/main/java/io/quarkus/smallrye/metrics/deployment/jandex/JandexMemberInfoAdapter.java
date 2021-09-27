package io.quarkus.smallrye.metrics.deployment.jandex;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.smallrye.metrics.deployment.SmallRyeMetricsDotNames;
import io.smallrye.metrics.elementdesc.AnnotationInfo;
import io.smallrye.metrics.elementdesc.MemberInfo;
import io.smallrye.metrics.elementdesc.MemberType;
import io.smallrye.metrics.elementdesc.RawMemberInfo;
import io.smallrye.metrics.elementdesc.adapter.MemberInfoAdapter;

public class JandexMemberInfoAdapter implements MemberInfoAdapter<AnnotationTarget> {

    private final IndexView indexView;
    private final TransformedAnnotationsBuildItem transformedAnnotations;

    public JandexMemberInfoAdapter(IndexView indexView, TransformedAnnotationsBuildItem transformedAnnotations) {
        this.indexView = indexView;
        this.transformedAnnotations = transformedAnnotations;
    }

    @Override
    public MemberInfo convert(AnnotationTarget input) {
        MemberType memberType;
        AnnotationTarget.Kind kind = input.kind();
        if (kind.equals(AnnotationTarget.Kind.FIELD)) {
            memberType = MemberType.FIELD;
        } else {
            if (input.asMethod().name().equals("<init>")) {
                memberType = MemberType.CONSTRUCTOR;
            } else {
                memberType = MemberType.METHOD;
            }
        }

        final List<AnnotationInfo> annotationInformation;
        final String name, declaringClassSimpleName, declaringClassName;
        final String[] parameterTypeNames;
        JandexAnnotationInfoAdapter annotationInfoAdapter = new JandexAnnotationInfoAdapter(indexView);
        if (input.kind().equals(AnnotationTarget.Kind.METHOD)) {
            declaringClassName = input.asMethod().declaringClass().name().toString();
            declaringClassSimpleName = input.asMethod().declaringClass().simpleName();
            name = input.asMethod().name();
            annotationInformation = transformedAnnotations.getAnnotations(input)
                    .stream()
                    .filter(SmallRyeMetricsDotNames::isMetricAnnotation)
                    .map(annotationInfoAdapter::convert)
                    .collect(Collectors.toList());
            parameterTypeNames = input.asMethod().parameters().stream()
                    .map(type -> type.name().toString())
                    .toArray(String[]::new);
        } else {
            declaringClassName = input.asField().declaringClass().name().toString();
            declaringClassSimpleName = input.asField().declaringClass().simpleName();
            name = input.asField().name();
            annotationInformation = transformedAnnotations.getAnnotations(input)
                    .stream()
                    .filter(SmallRyeMetricsDotNames::isMetricAnnotation)
                    .map(annotationInfoAdapter::convert)
                    .collect(Collectors.toList());
            parameterTypeNames = new String[0];
        }

        return new RawMemberInfo(memberType,
                declaringClassName,
                declaringClassSimpleName,
                name,
                annotationInformation,
                parameterTypeNames);
    }
}
