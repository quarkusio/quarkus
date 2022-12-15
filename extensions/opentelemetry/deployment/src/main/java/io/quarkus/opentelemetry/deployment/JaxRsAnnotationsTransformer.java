package io.quarkus.opentelemetry.deployment;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.jaxrs.JaxRsBinding;

public class JaxRsAnnotationsTransformer implements AnnotationsTransformer {

    static final DotName ANNOT_GET = DotName.createSimple(javax.ws.rs.GET.class);
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
        if (methodInfo.hasAnnotation(ANNOT_GET)) {
            transformationContext.transform()
                    .add(JAX_RS_INTERCEPTOR_BINDING)
                    .done();
        }
    }
}
