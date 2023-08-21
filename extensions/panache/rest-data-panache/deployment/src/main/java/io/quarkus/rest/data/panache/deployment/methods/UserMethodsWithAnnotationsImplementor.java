package io.quarkus.rest.data.panache.deployment.methods;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;

/**
 * Propagate all the user methods that have annotations.
 *
 * This is necessary when users use annotations with an interceptor binding like `@Transactional`.
 */
public final class UserMethodsWithAnnotationsImplementor implements MethodImplementor {

    @Override
    public void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        if (resourceMetadata.getResourceInterface() != null) {
            for (var methodInfo : resourceMetadata.getResourceInterface().methods()) {
                if (methodInfo.isDefault() && !methodInfo.annotations().isEmpty()) {
                    MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.of(methodInfo));
                    methodCreator.setSignature(methodInfo.genericSignatureIfRequired());
                    for (var annotation : methodInfo.annotations()) {
                        if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                            short position = annotation.target().asMethodParameter().position();
                            methodCreator.getParameterAnnotations(position).addAnnotation(annotation);
                        }

                        if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                            methodCreator.addAnnotation(annotation);
                        }
                    }

                    ResultHandle[] params = new ResultHandle[methodInfo.parametersCount()];
                    for (int paramIdx = 0; paramIdx < methodInfo.parametersCount(); paramIdx++) {
                        params[paramIdx] = methodCreator.getMethodParam(paramIdx);
                    }

                    methodCreator.returnValue(
                            methodCreator.invokeSpecialInterfaceMethod(methodInfo, methodCreator.getThis(), params));
                    methodCreator.close();
                }
            }
        }
    }
}
