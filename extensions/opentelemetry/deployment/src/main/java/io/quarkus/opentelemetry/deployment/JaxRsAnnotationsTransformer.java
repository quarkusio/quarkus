package io.quarkus.opentelemetry.deployment;

import java.util.List;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.jaxrs.JaxRsBinding;

public class JaxRsAnnotationsTransformer implements AnnotationsTransformer {

    static final DotName ANNOT_DELETE = DotName.createSimple(javax.ws.rs.DELETE.class);
    static final DotName ANNOT_GET = DotName.createSimple(javax.ws.rs.GET.class);
    static final DotName ANNOT_HEAD = DotName.createSimple(javax.ws.rs.HEAD.class);
    static final DotName ANNOT_OPTIONS = DotName.createSimple(javax.ws.rs.OPTIONS.class);
    static final DotName ANNOT_PATCH = DotName.createSimple(javax.ws.rs.PATCH.class);
    static final DotName ANNOT_POST = DotName.createSimple(javax.ws.rs.POST.class);
    static final DotName ANNOT_PUT = DotName.createSimple(javax.ws.rs.PUT.class);
    static final List<DotName> JAX_RS_ANNOTATIONS_LIST = List.of(
            ANNOT_DELETE, ANNOT_GET, ANNOT_HEAD, ANNOT_OPTIONS, ANNOT_PATCH, ANNOT_POST, ANNOT_PUT);
    private static final DotName JAX_RS_INTERCEPTOR_BINDING = DotName.createSimple(JaxRsBinding.class);

    @Override
    public boolean appliesTo(Kind kind) {
        return kind == AnnotationTarget.Kind.METHOD;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        AnnotationTarget target = transformationContext.getTarget();
        if (target.kind() != AnnotationTarget.Kind.METHOD) {
            return;
        }
        MethodInfo methodInfo = target.asMethod();
        for (DotName currExpectedAnnotation : JAX_RS_ANNOTATIONS_LIST) {
            if (methodInfo.hasAnnotation(currExpectedAnnotation)) {
                transformationContext.transform()
                        .add(JAX_RS_INTERCEPTOR_BINDING)
                        .done();
                break; // out of loop
            }
        }
    }
}
