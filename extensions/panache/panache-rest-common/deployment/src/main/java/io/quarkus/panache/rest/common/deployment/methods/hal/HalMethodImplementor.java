package io.quarkus.panache.rest.common.deployment.methods.hal;

import java.util.Collection;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.methods.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.MethodMetadata;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceName;
import io.quarkus.panache.rest.common.runtime.hal.HalCollectionWrapper;
import io.quarkus.panache.rest.common.runtime.hal.HalEntityWrapper;

abstract class HalMethodImplementor implements MethodImplementor {

    @Override
    public void implement(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor propertiesAccessor,
            PanacheCrudResourceInfo resourceInfo) {
        if (propertiesAccessor.isExposed(resourceInfo.getResourceClassInfo(), getStandardMethodMetadata(resourceInfo))) {
            implementInternal(classCreator, index, propertiesAccessor, resourceInfo);
        }
    }

    protected abstract void implementInternal(ClassCreator classCreator, IndexView index,
            OperationPropertiesAccessor propertiesAccessor, PanacheCrudResourceInfo resourceInfo);

    protected abstract MethodMetadata getStandardMethodMetadata(PanacheCrudResourceInfo resourceInfo);

    protected ResultHandle wrapHalEntity(BytecodeCreator creator, ResultHandle entity) {
        return creator.newInstance(MethodDescriptor.ofConstructor(HalEntityWrapper.class, Object.class), entity);
    }

    protected ResultHandle wrapEntities(BytecodeCreator creator, ResultHandle entities, PanacheCrudResourceInfo resourceInfo) {
        String collectionName = ResourceName.fromClass(resourceInfo.getResourceClassInfo().simpleName());
        return creator.newInstance(
                MethodDescriptor.ofConstructor(HalCollectionWrapper.class, Collection.class, Class.class, String.class),
                entities, creator.loadClass(resourceInfo.getEntityClassName()), creator.load(collectionName));
    }
}
