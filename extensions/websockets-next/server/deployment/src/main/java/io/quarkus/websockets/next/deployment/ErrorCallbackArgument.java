package io.quarkus.websockets.next.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ResultHandle;

class ErrorCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.callbackAnnotation().name().equals(WebSocketDotNames.ON_ERROR)
                && isThrowable(context.index(), context.parameter().type().name());
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        return context.getPayload();
    }

    boolean isThrowable(IndexView index, DotName clazzName) {
        if (clazzName.equals(WebSocketDotNames.THROWABLE)) {
            return true;
        }
        ClassInfo clazz = index.getClassByName(clazzName);
        if (clazz == null) {
            throw new IllegalArgumentException("The class " + clazzName + " not found in the index");
        }
        if (clazz.superName().equals(DotName.OBJECT_NAME)
                || clazz.superName().equals(DotName.RECORD_NAME)
                || clazz.superName().equals(DotName.ENUM_NAME)) {
            return false;
        }
        if (clazz.superName().equals(WebSocketDotNames.THROWABLE)) {
            return true;
        }
        return isThrowable(index, clazz.superName());
    }

    public static boolean isError(CallbackArgument callbackArgument) {
        return callbackArgument instanceof ErrorCallbackArgument;
    }

}
