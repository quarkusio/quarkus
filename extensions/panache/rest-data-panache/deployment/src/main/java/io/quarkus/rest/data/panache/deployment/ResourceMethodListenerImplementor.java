package io.quarkus.rest.data.panache.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;

public class ResourceMethodListenerImplementor {
    private static final String ON_AFTER = "onAfter";
    private static final String ON_BEFORE_ADD_METHOD_NAME = "onBeforeAdd";
    private static final String ON_AFTER_ADD_METHOD_NAME = ON_AFTER + "Add";
    private static final String ON_BEFORE_UPDATE_METHOD_NAME = "onBeforeUpdate";
    private static final String ON_AFTER_UPDATE_METHOD_NAME = ON_AFTER + "Update";
    private static final String ON_BEFORE_DELETE_METHOD_NAME = "onBeforeDelete";
    private static final String ON_AFTER_DELETE_METHOD_NAME = ON_AFTER + "Delete";

    private final Map<FieldDescriptor, ClassInfo> listenerFields = new HashMap<>();
    private final boolean isHibernateReactive;

    public ResourceMethodListenerImplementor(ClassCreator cc, List<ClassInfo> resourceMethodListeners,
            boolean isHibernateReactive) {
        this.isHibernateReactive = isHibernateReactive;
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

    public void onAfterAdd(BytecodeCreator methodCreator, ResultHandle entity) {
        invokeMethodUsingEntity(ON_AFTER_ADD_METHOD_NAME, methodCreator, entity);
    }

    public void onBeforeUpdate(BytecodeCreator methodCreator, ResultHandle entity) {
        invokeMethodUsingEntity(ON_BEFORE_UPDATE_METHOD_NAME, methodCreator, entity);
    }

    public void onAfterUpdate(BytecodeCreator methodCreator, ResultHandle entity) {
        invokeMethodUsingEntity(ON_AFTER_UPDATE_METHOD_NAME, methodCreator, entity);
    }

    public void onBeforeDelete(BytecodeCreator methodCreator, ResultHandle id) {
        invokeMethodUsingId(ON_BEFORE_DELETE_METHOD_NAME, methodCreator, id);
    }

    public void onAfterDelete(BytecodeCreator methodCreator, ResultHandle id) {
        invokeMethodUsingId(ON_AFTER_DELETE_METHOD_NAME, methodCreator, id);
    }

    private void invokeMethodUsingEntity(String methodName, BytecodeCreator methodCreator, ResultHandle entity) {
        processEventListener(methodName, methodCreator, (eventListener, method) -> {
            if (isUsingHibernateReactiveAndOnAfterMethod(methodName)) {
                UniImplementor.subscribeWith(methodCreator, entity,
                        (lambda, item) -> {
                            lambda.invokeVirtualMethod(method, eventListener, item);
                            lambda.returnNull();
                        });
            } else {
                methodCreator.invokeVirtualMethod(method, eventListener, entity);
            }
        });
    }

    private void invokeMethodUsingId(String methodName, BytecodeCreator methodCreator, ResultHandle id) {
        processEventListener(methodName, methodCreator, (eventListener, method) -> {
            methodCreator.invokeVirtualMethod(method, eventListener, id);
        });
    }

    private boolean isUsingHibernateReactiveAndOnAfterMethod(String methodName) {
        return isHibernateReactive && methodName.startsWith(ON_AFTER);
    }

    private void processEventListener(String methodName, BytecodeCreator methodCreator,
            BiConsumer<ResultHandle, MethodInfo> apply) {
        for (Map.Entry<FieldDescriptor, ClassInfo> eventListenerEntry : listenerFields.entrySet()) {
            MethodInfo method = findMethodByName(eventListenerEntry.getValue(), methodName);
            if (method != null) {
                // If method is implemented
                ResultHandle eventListener = methodCreator.readInstanceField(eventListenerEntry.getKey(),
                        methodCreator.getThis());
                apply.accept(eventListener, method);
            }
        }
    }

    private MethodInfo findMethodByName(ClassInfo classInfo, String methodName) {
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
