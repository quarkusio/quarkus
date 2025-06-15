package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.ResourceMethodListenerImplementor;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;

public class HibernateReactiveResourceMethodListenerImplementor extends ResourceMethodListenerImplementor {

    public HibernateReactiveResourceMethodListenerImplementor(ClassCreator cc,
            List<ClassInfo> resourceMethodListeners) {
        super(cc, resourceMethodListeners);
    }

    public ResultHandle onAfterAdd(BytecodeCreator methodCreator, ResultHandle uni) {
        return invokeUniMethodUsingEntity(ON_AFTER_ADD_METHOD_NAME, methodCreator, uni);
    }

    public ResultHandle onAfterUpdate(BytecodeCreator methodCreator, ResultHandle uni) {
        return invokeUniMethodUsingEntity(ON_AFTER_UPDATE_METHOD_NAME, methodCreator, uni);
    }

    public ResultHandle onAfterDelete(BytecodeCreator methodCreator, ResultHandle uni, ResultHandle id) {
        return invokeUniMethodUsingId(ON_AFTER_DELETE_METHOD_NAME, methodCreator, uni, id);
    }

    protected ResultHandle invokeUniMethodUsingEntity(String methodName, BytecodeCreator methodCreator,
            ResultHandle uni) {
        if (!hasListenerForMethod(methodName)) {
            return uni;
        }
        return UniImplementor.invoke(methodCreator, uni,
                (lambda, item) -> processEventListener(methodName, lambda, methodCreator.getThis(), item));
    }

    protected ResultHandle invokeUniMethodUsingId(String methodName, BytecodeCreator methodCreator, ResultHandle uni,
            ResultHandle id) {
        if (!hasListenerForMethod(methodName)) {
            return uni;
        }
        return UniImplementor.invoke(methodCreator, uni,
                (lambda, voidItem) -> processEventListener(methodName, lambda, methodCreator.getThis(), id));
    }

    private boolean hasListenerForMethod(String methodName) {
        for (Map.Entry<FieldDescriptor, ClassInfo> eventListenerEntry : listenerFields.entrySet()) {
            MethodInfo method = findMethodByName(eventListenerEntry.getValue(), methodName);
            if (method != null) {
                return true;
            }
        }
        return false;
    }
}
