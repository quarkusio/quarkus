package org.jboss.resteasy.reactive.server.processor.generation.multipart;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

public final class GeneratorUtils {

    private GeneratorUtils() {
    }

    public static void paramHandleFromReqContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, DotName returnType) {
        paramHandleFromReqContextMethod(m, qrReqCtxHandle, targetMethodParamHandles, i, methodName,
                returnType.toString());
    }

    public static void paramHandleFromReqContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, Class<?> returnType) {
        paramHandleFromReqContextMethod(m, qrReqCtxHandle, targetMethodParamHandles, i, methodName,
                returnType.getName());
    }

    private static void paramHandleFromReqContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, String returnType) {
        targetMethodParamHandles[i] = m.invokeVirtualMethod(
                ofMethod(ResteasyReactiveRequestContext.class.getName(), methodName, returnType), qrReqCtxHandle);
    }

    public static ResultHandle unwrapObject(MethodCreator m, ResultHandle qrReqCtxHandle, DotName classType) {
        return m.invokeVirtualMethod(
                ofMethod(ResteasyReactiveRequestContext.class, "unwrap", Object.class, Class.class), qrReqCtxHandle,
                m.loadClass(classType.toString()));
    }

    public static ResultHandle runtimeResourceHandle(MethodCreator filterMethod, ResultHandle qrReqCtxHandle) {
        return filterMethod.invokeVirtualMethod(
                ofMethod(ResteasyReactiveRequestContext.class, "getTarget", RuntimeResource.class), qrReqCtxHandle);
    }
}
