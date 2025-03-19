package io.quarkus.rest.data.panache.deployment.methods;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.Capabilities;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;

/**
 * Override the user method defined in the interface to propagate the annotations.
 * In case of the method is also a REST resource, it will be configured to support the rest-data features like hal.
 */
public class OverrideUserMethodImplementor extends StandardMethodImplementor {

    static final DotName REST_PATH = DotName.createSimple(Path.class);
    static final DotName REST_PRODUCES = DotName.createSimple(Produces.class);

    private final MethodInfo methodInfo;

    protected OverrideUserMethodImplementor(MethodInfo methodInfo, Capabilities capabilities) {
        super(capabilities);

        this.methodInfo = methodInfo;
    }

    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.of(methodInfo));
        methodCreator.setSignature(methodInfo.genericSignatureIfRequired());
        Set<String> produces = new HashSet<>();
        for (var annotation : methodInfo.annotations()) {
            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                short position = annotation.target().asMethodParameter().position();
                methodCreator.getParameterAnnotations(position).addAnnotation(annotation);
            }

            if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                if (REST_PRODUCES.equals(annotation.name())) {
                    var annotationValue = annotation.value();
                    if (annotationValue != null) {
                        produces.addAll(List.of(annotationValue.asStringArray()));
                    }
                } else {
                    methodCreator.addAnnotation(annotation);
                }
            }
        }

        ResultHandle[] params = new ResultHandle[methodInfo.parametersCount()];
        for (int paramIdx = 0; paramIdx < methodInfo.parametersCount(); paramIdx++) {
            params[paramIdx] = methodCreator.getMethodParam(paramIdx);
        }

        // Special handling for user-defined REST methods
        if (methodInfo.hasAnnotation(REST_PATH)) {
            addLinksAnnotation(methodCreator, resourceProperties, resourceMetadata.getEntityType(), getResourceMethodName());
            addSecurityAnnotations(methodCreator, resourceProperties);
            addProducesAnnotation(produces, methodCreator, resourceProperties);
        }

        methodCreator.returnValue(
                methodCreator.invokeSpecialInterfaceMethod(methodInfo, methodCreator.getThis(), params));
        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return methodInfo.name();
    }

    @Override
    protected void addLinksAnnotation(AnnotatedElement element, ResourceProperties resourceProperties, String entityClassName,
            String rel) {
        if (!resourceProperties.isHal()) {
            return;
        }

        String linksAnnotationName = "io.quarkus.resteasy.reactive.links.RestLink";
        // Add the links annotation if and only if the user didn't add it.
        if (!methodInfo.hasAnnotation(linksAnnotationName)) {
            super.addLinksAnnotation(element, resourceProperties, entityClassName, rel);
        }
    }

    private void addProducesAnnotation(Set<String> definedProduces, MethodCreator methodCreator,
            ResourceProperties resourceProperties) {
        Set<String> produces = new HashSet<>(definedProduces);
        produces.add(APPLICATION_JSON);
        if (resourceProperties.isHal()) {
            produces.add(APPLICATION_HAL_JSON);
        }

        addProducesAnnotation(methodCreator, produces.toArray(new String[0]));
    }
}
