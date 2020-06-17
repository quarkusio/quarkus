package io.quarkus.rest.data.panache.deployment.methods;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;

abstract class StandardMethodImplementor implements MethodImplementor {

    @Override
    public void implement(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        if (propertiesAccessor.isExposed(resourceInfo.getType(), methodMetadata)) {
            implementInternal(classCreator, index, propertiesAccessor, resourceInfo);
        } else {
            NotExposedMethodImplementor implementor = new NotExposedMethodImplementor(methodMetadata);
            implementor.implement(classCreator, index, propertiesAccessor, resourceInfo);
        }
    }

    protected abstract void implementInternal(ClassCreator classCreator, IndexView index,
            MethodPropertiesAccessor propertiesAccessor, RestDataResourceInfo resourceInfo);

    protected abstract MethodMetadata getMethodMetadata(RestDataResourceInfo resourceInfo);

}
