package io.quarkus.rest.data.panache.deployment.methods;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;

/**
 * Propagate the user methods annotated with `@Transactional`.
 * This implementor is only used if Hibernate ORM is present.
 */
public final class UserMethodsWithTransactionalImplementor implements MethodImplementor {

    public static final DotName TRANSACTIONAL = DotName.createSimple("jakarta.transaction.Transactional");

    private final Capabilities capabilities;

    public UserMethodsWithTransactionalImplementor(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        if (capabilities.isPresent(Capability.HIBERNATE_ORM) && resourceMetadata.getResourceInterface() != null) {
            for (var methodInfo : resourceMetadata.getResourceInterface().methods()) {
                // we only need to propagate the user methods annotated with `@Transactional`
                if (methodInfo.hasAnnotation(TRANSACTIONAL)) {
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
