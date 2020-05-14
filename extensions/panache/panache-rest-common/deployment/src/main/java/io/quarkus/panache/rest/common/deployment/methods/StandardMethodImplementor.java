package io.quarkus.panache.rest.common.deployment.methods;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;

abstract class StandardMethodImplementor implements MethodImplementor {

    @Override
    public void implement(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor propertiesAccessor,
            PanacheCrudResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        if (propertiesAccessor.isExposed(resourceInfo.getResourceClassInfo(), methodMetadata)) {
            implementInternal(classCreator, index, propertiesAccessor, resourceInfo);
        } else {
            NotExposedMethodImplementor implementor = new NotExposedMethodImplementor(methodMetadata);
            implementor.implement(classCreator, index, propertiesAccessor, resourceInfo);
        }
    }

    protected abstract void implementInternal(ClassCreator classCreator, IndexView index,
            OperationPropertiesAccessor propertiesAccessor, PanacheCrudResourceInfo resourceInfo);

    protected abstract MethodMetadata getMethodMetadata(PanacheCrudResourceInfo resourceInfo);

}
