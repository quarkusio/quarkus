package io.quarkus.rest.data.panache.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;

public abstract class ResourceMethodListenerImplementor {
    protected static final String ON_AFTER = "onAfter";
    protected static final String ON_BEFORE_ADD_METHOD_NAME = "onBeforeAdd";
    protected static final String ON_AFTER_ADD_METHOD_NAME = ON_AFTER + "Add";
    protected static final String ON_BEFORE_UPDATE_METHOD_NAME = "onBeforeUpdate";
    protected static final String ON_AFTER_UPDATE_METHOD_NAME = ON_AFTER + "Update";
    protected static final String ON_BEFORE_DELETE_METHOD_NAME = "onBeforeDelete";
    protected static final String ON_AFTER_DELETE_METHOD_NAME = ON_AFTER + "Delete";

    protected final Map<FieldDescriptor, ClassInfo> listenerFields = new HashMap<>();

    public ResourceMethodListenerImplementor(ClassCreator cc, List<ClassInfo> resourceMethodListeners) {
        for (int index = 0; index < resourceMethodListeners.size(); index++) {
            ClassInfo eventListenerClass = resourceMethodListeners.get(index);
            FieldCreator delegateField = cc.getFieldCreator("listener" + index, eventListenerClass.name().toString())
                    .setModifiers(Modifier.PROTECTED);
            delegateField.addAnnotation(Inject.class);

            listenerFields.put(delegateField.getFieldDescriptor(), eventListenerClass);
        }
    }

    public void onBeforeAdd(BytecodeCreator methodCreator, ResultHandle entity) {
        invokeMethodUsingEntity(ON_BEFORE_ADD_METHOD_NAME, methodCreator, entity);
    }

    public void onBeforeUpdate(BytecodeCreator methodCreator, ResultHandle entity) {
        invokeMethodUsingEntity(ON_BEFORE_UPDATE_METHOD_NAME, methodCreator, entity);
    }

    public void onBeforeDelete(BytecodeCreator methodCreator, ResultHandle id) {
        invokeMethodUsingId(ON_BEFORE_DELETE_METHOD_NAME, methodCreator, id);
    }

    protected void invokeMethodUsingEntity(String methodName, BytecodeCreator methodCreator, ResultHandle entity) {
        processEventListener(methodName, methodCreator, methodCreator.getThis(), entity);
    }

    protected void invokeMethodUsingId(String methodName, BytecodeCreator methodCreator, ResultHandle id) {
        processEventListener(methodName, methodCreator, methodCreator.getThis(), id);
    }

    protected void processEventListener(String methodName, BytecodeCreator methodCreator,
            ResultHandle eventListenerContainer, ResultHandle parameter) {
        for (Map.Entry<FieldDescriptor, ClassInfo> eventListenerEntry : listenerFields.entrySet()) {
            MethodInfo method = findMethodByName(eventListenerEntry.getValue(), methodName);
            if (method != null) {
                // If method is implemented
                ResultHandle eventListener = methodCreator.readInstanceField(eventListenerEntry.getKey(),
                        eventListenerContainer);
                methodCreator.invokeVirtualMethod(method, eventListener, parameter);
            }
        }
    }

    protected MethodInfo findMethodByName(ClassInfo classInfo, String methodName) {
        List<MethodInfo> methods = classInfo.methods();
        for (int index = 0; index < methods.size(); index++) {
            MethodInfo method = methods.get(index);
            if (methodName.equals(method.name())) {
                return method;
            }
        }

        return null;
    }
}
