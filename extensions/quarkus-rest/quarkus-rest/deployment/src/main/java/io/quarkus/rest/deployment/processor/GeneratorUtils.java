package io.quarkus.rest.deployment.processor;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import org.jboss.jandex.DotName;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.mapping.RuntimeResource;
import io.vertx.ext.web.RoutingContext;

final class GeneratorUtils {

    private GeneratorUtils() {
    }

    static void paramHandleFromReqContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, DotName returnType) {
        paramHandleFromReqContextMethod(m, qrReqCtxHandle, targetMethodParamHandles, i, methodName, returnType.toString());
    }

    static void paramHandleFromReqContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, Class<?> returnType) {
        paramHandleFromReqContextMethod(m, qrReqCtxHandle, targetMethodParamHandles, i, methodName, returnType.getName());
    }

    private static void paramHandleFromReqContextMethod(MethodCreator m, ResultHandle qrReqCtxHandle,
            ResultHandle[] targetMethodParamHandles, int i, String methodName, String returnType) {
        targetMethodParamHandles[i] = m.invokeVirtualMethod(
                ofMethod(QuarkusRestRequestContext.class.getName(), methodName, returnType), qrReqCtxHandle);
    }

    static ResultHandle routingContextHandler(MethodCreator m, ResultHandle qrReqCtxHandle) {
        return m.invokeVirtualMethod(
                ofMethod(QuarkusRestRequestContext.class, "getContext", RoutingContext.class), qrReqCtxHandle);
    }

    static ResultHandle runtimeResourceHandle(MethodCreator filterMethod, ResultHandle qrReqCtxHandle) {
        return filterMethod.invokeVirtualMethod(
                ofMethod(QuarkusRestRequestContext.class, "getTarget", RuntimeResource.class), qrReqCtxHandle);
    }
}
