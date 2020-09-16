package io.quarkus.rest.data.panache.deployment.methods.hal;

import java.util.Collection;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.methods.StandardMethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.ResourceName;
import io.quarkus.rest.data.panache.runtime.hal.HalCollectionWrapper;
import io.quarkus.rest.data.panache.runtime.hal.HalEntityWrapper;

abstract class HalMethodImplementor extends StandardMethodImplementor {

    @Override
    public void implement(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        if (propertiesAccessor.isExposed(resourceInfo.getType(), getMethodMetadata(resourceInfo))) {
            implementInternal(classCreator, index, propertiesAccessor, resourceInfo);
        }
    }

    protected ResultHandle wrapHalEntity(BytecodeCreator creator, ResultHandle entity) {
        return creator.newInstance(MethodDescriptor.ofConstructor(HalEntityWrapper.class, Object.class), entity);
    }

    protected ResultHandle wrapHalEntities(BytecodeCreator creator, ResultHandle entities, RestDataResourceInfo resourceInfo) {
        String collectionName = ResourceName.fromClass(resourceInfo.getType());
        return creator.newInstance(
                MethodDescriptor.ofConstructor(HalCollectionWrapper.class, Collection.class, Class.class, String.class),
                entities, creator.loadClass(resourceInfo.getEntityInfo().getType()), creator.load(collectionName));
    }
}
