package io.quarkus.panache.rest.common.deployment.methods;

import javax.ws.rs.core.Response;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;

final class NotExposedMethodImplementor implements MethodImplementor {

    private final MethodMetadata methodMetadata;

    public NotExposedMethodImplementor(MethodMetadata methodMetadata) {
        this.methodMetadata = methodMetadata;
    }

    @Override
    public void implement(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor accessor,
            PanacheCrudResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator
                .getMethodCreator(methodMetadata.getName(), Response.class.getName(), methodMetadata.getParameterTypes());
        methodCreator
                .throwException(RuntimeException.class, String.format("'%s' method is not exposed", methodMetadata.getName()));
        methodCreator.close();
    }
}
