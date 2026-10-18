package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import java.util.List;

import org.jboss.jandex.ClassInfo;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.ResourceMethodListenerImplementor;

public class HibernateORMResourceMethodListenerImplementor extends ResourceMethodListenerImplementor {

    public HibernateORMResourceMethodListenerImplementor(ClassCreator cc, List<ClassInfo> resourceMethodListeners) {
        super(cc, resourceMethodListeners);
    }

    public void onAfterAdd(BytecodeCreator methodCreator, ResultHandle entity) {
        invokeMethodUsingEntity(ON_AFTER_ADD_METHOD_NAME, methodCreator, entity);
    }

    public void onAfterUpdate(BytecodeCreator methodCreator, ResultHandle entity) {
        invokeMethodUsingEntity(ON_AFTER_UPDATE_METHOD_NAME, methodCreator, entity);
    }

    public void onAfterDelete(BytecodeCreator methodCreator, ResultHandle id) {
        invokeMethodUsingId(ON_AFTER_DELETE_METHOD_NAME, methodCreator, id);
    }

}
