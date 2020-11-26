package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_REQUEST;
import static io.quarkus.resteasy.reactive.server.deployment.GeneratorUtils.paramHandleFromReqContextMethod;
import static io.quarkus.resteasy.reactive.server.deployment.GeneratorUtils.routingContextHandler;
import static io.quarkus.resteasy.reactive.server.deployment.GeneratorUtils.runtimeResourceHandle;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTAINER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HTTP_HEADERS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.URI_INFO;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.core.LazyMethod;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestContainerRequestContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestHttpHeaders;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.SimplifiedResourceInfo;

import io.quarkus.arc.Unremovable;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

final class ClassLevelExceptionMapperGenerator {

    private ClassLevelExceptionMapperGenerator() {
    }

    /**
     * Generates an exception mapper that delegates exception handling to a method of a Resource class.
     * For example, for a method like:
     * 
     * <pre>
     * &#64;ExceptionMapper(IllegalArgumentException.class)
     * public Response perClassMapper(IllegalArgumentException e, Request r) {
     *
     * }
     * </pre>
     *
     * a generated exception mapper would look like this (only the actual called method is displayed)
     *
     * <pre>
     * &#64;Singleton
     * &#64;Unremovable
     * public class GreetingResource$GeneratedExceptionHandlerFor$IllegalArgumentException$OfMethod$perClassMapper
     *         implements QuarkusRestExceptionMapper {
     *
     *     public Response toResponse(IllegalArgumentException e, QuarkusRestRequestContext ctx) {
     *         GreetingResource instance = (GreetingResource) ctx.getEndpointInstance();
     *         return instance.perClassMapper(e, ctx.getRequest());
     *     }
     * }
     * </pre>
     *
     * @return a map containing the exception class name as the key and the generated exception mapper as the value
     */
    public static Map<String, String> generate(MethodInfo targetMethod, ClassOutput classOutput) {
        checkReturnType(targetMethod);
        checkModifiers(targetMethod);

        ClassInfo targetClass = targetMethod.declaringClass();
        AnnotationInstance exceptionMapperInstance = targetMethod
                .annotation(ResteasyReactiveDotNames.EXCEPTION_MAPPER_ANNOTATION);
        Type[] handledExceptionTypes = exceptionMapperInstance.value().asClassArray();
        Map<String, String> result = new HashMap<>();
        for (Type handledExceptionType : handledExceptionTypes) {
            String generatedClassName = getGeneratedClassName(targetMethod, handledExceptionType);
            try (ClassCreator cc = ClassCreator.builder().className(generatedClassName)
                    .interfaces(QuarkusRestExceptionMapper.class).classOutput(classOutput).build()) {
                cc.addAnnotation(Singleton.class);
                cc.addAnnotation(Unremovable.class);

                // ctor
                MethodCreator ctor = cc.getMethodCreator("<init>", "V");
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ctor.returnValue(null);

                MethodDescriptor specToResponseDescriptor = MethodDescriptor.ofMethod(generatedClassName, "toResponse",
                        Response.class.getName(),
                        handledExceptionType.name().toString());

                // spec toResponse(...) method
                MethodCreator specToResponse = cc.getMethodCreator(specToResponseDescriptor);
                specToResponse.throwException(IllegalStateException.class, "This should never be called");

                // bridge toResponse(Throwable) method
                MethodCreator bridgeSpecToResponse = cc.getMethodCreator("toResponse", Response.class, Throwable.class);
                ResultHandle bridgeSpecParam = bridgeSpecToResponse.getMethodParam(0);
                ResultHandle castedBridgeSpecMethodParam = bridgeSpecToResponse.checkCast(bridgeSpecParam,
                        handledExceptionType.name().toString());
                bridgeSpecToResponse.returnValue(bridgeSpecToResponse.invokeVirtualMethod(specToResponseDescriptor,
                        bridgeSpecToResponse.getThis(), castedBridgeSpecMethodParam));

                MethodDescriptor qrToResponseDescriptor = MethodDescriptor.ofMethod(generatedClassName, "toResponse",
                        Response.class.getName(),
                        handledExceptionType.name().toString(), ResteasyReactiveRequestContext.class.getName());

                // bridge toResponse(Throwable, QuarkusRestRequestContext) method
                MethodCreator bridgeQRToResponse = cc.getMethodCreator("toResponse", Response.class, Throwable.class,
                        ResteasyReactiveRequestContext.class);
                ResultHandle bridgeQRExceptionParam = bridgeQRToResponse.getMethodParam(0);
                ResultHandle bridgeQRContextParam = bridgeQRToResponse.getMethodParam(1);
                ResultHandle castedBridgeQRMethodParam = bridgeQRToResponse.checkCast(bridgeQRExceptionParam,
                        handledExceptionType.name().toString());
                bridgeQRToResponse.returnValue(bridgeQRToResponse.invokeVirtualMethod(qrToResponseDescriptor,
                        bridgeQRToResponse.getThis(), castedBridgeQRMethodParam, bridgeQRContextParam));

                // Quarkus REST toResponse(...) method
                MethodCreator qrToResponse = cc.getMethodCreator(qrToResponseDescriptor);
                ResultHandle exceptionHandle = qrToResponse.getMethodParam(0);
                ResultHandle contextHandle = qrToResponse.getMethodParam(1);
                ResultHandle endpointInstanceHandle = qrToResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(ResteasyReactiveRequestContext.class, "getEndpointInstance", Object.class),
                        contextHandle);
                ResultHandle targetInstanceHandle = qrToResponse.checkCast(endpointInstanceHandle,
                        targetClass.name().toString());
                if (targetMethod.parameters().isEmpty()) {
                    // just call the target method with no parameters
                    ResultHandle resultHandle = qrToResponse.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(targetClass.name().toString(), targetMethod.name(), Response.class),
                            targetInstanceHandle);
                    qrToResponse.returnValue(resultHandle);
                } else {
                    List<Type> parameters = targetMethod.parameters();
                    ResultHandle[] targetMethodParamHandles = new ResultHandle[parameters.size()];
                    String[] parameterTypes = new String[parameters.size()];
                    // TODO: we probably want to refactor this and remove duplicate code that also exists in CustomProviderGenerator
                    for (int i = 0; i < parameters.size(); i++) {
                        Type parameter = parameters.get(i);
                        DotName paramDotName = parameter.name();
                        parameterTypes[i] = paramDotName.toString();
                        if (paramDotName.equals(handledExceptionType.name())) {
                            targetMethodParamHandles[i] = exceptionHandle;
                        } else if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)
                                || ResteasyReactiveServerDotNames.QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                            targetMethodParamHandles[i] = qrToResponse.invokeVirtualMethod(
                                    ofMethod(ResteasyReactiveRequestContext.class.getName(), "getContainerRequestContext",
                                            QuarkusRestContainerRequestContextImpl.class),
                                    contextHandle);
                        } else if (URI_INFO.equals(paramDotName)) {
                            paramHandleFromReqContextMethod(qrToResponse, contextHandle, targetMethodParamHandles, i,
                                    "getUriInfo",
                                    URI_INFO);
                        } else if (HTTP_HEADERS.equals(paramDotName)) {
                            paramHandleFromReqContextMethod(qrToResponse, contextHandle, targetMethodParamHandles, i,
                                    "getHttpHeaders",
                                    QuarkusRestHttpHeaders.class);
                        } else if (REQUEST.equals(paramDotName)) {
                            paramHandleFromReqContextMethod(qrToResponse, contextHandle, targetMethodParamHandles, i,
                                    "getRequest",
                                    REQUEST);
                        } else if (HTTP_SERVER_REQUEST.equals(paramDotName)) {
                            ResultHandle routingContextHandle = routingContextHandler(qrToResponse, contextHandle);
                            targetMethodParamHandles[i] = qrToResponse.invokeInterfaceMethod(
                                    ofMethod(RoutingContext.class, "request", HttpServerRequest.class), routingContextHandle);
                        } else if (RESOURCE_INFO.equals(paramDotName)) {
                            ResultHandle runtimeResourceHandle = runtimeResourceHandle(qrToResponse, contextHandle);
                            targetMethodParamHandles[i] = qrToResponse.invokeVirtualMethod(
                                    ofMethod(RuntimeResource.class, "getLazyMethod", LazyMethod.class), runtimeResourceHandle);
                        } else if (ResteasyReactiveServerDotNames.SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                            ResultHandle runtimeResourceHandle = runtimeResourceHandle(qrToResponse, contextHandle);
                            targetMethodParamHandles[i] = qrToResponse.invokeVirtualMethod(
                                    ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimplifiedResourceInfo.class),
                                    runtimeResourceHandle);
                        } else {
                            String parameterName = targetMethod.parameterName(i);
                            throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                                    + " of class '" + targetClass.name()
                                    + "' is not allowed");
                        }
                    }

                    ResultHandle resultHandle = qrToResponse.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(targetClass.name().toString(), targetMethod.name(),
                                    Response.class.getName(), parameterTypes),
                            targetInstanceHandle, targetMethodParamHandles);
                    qrToResponse.returnValue(resultHandle);
                }
            }
            result.put(handledExceptionType.name().toString(), generatedClassName);
        }
        return result;
    }

    private static void checkReturnType(MethodInfo targetMethod) {
        if (!targetMethod.returnType().name().equals(ResteasyReactiveDotNames.RESPONSE)) {
            throw new RuntimeException("Method '" + targetMethod.name() + " of class '" + targetMethod.declaringClass().name()
                    + "' cannot be used as an exception mapper as it does not declare '"
                    + ResteasyReactiveDotNames.RESPONSE.toString() + "' as its return type");
        }
    }

    private static void checkModifiers(MethodInfo info) {
        if ((info.flags() & Modifier.PRIVATE) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be private as it is annotated with '@" + ResteasyReactiveDotNames.EXCEPTION_MAPPER_ANNOTATION
                    + "'");
        }
        if ((info.flags() & Modifier.STATIC) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be static as it is annotated with '@" + ResteasyReactiveDotNames.EXCEPTION_MAPPER_ANNOTATION
                    + "'");
        }
    }

    private static String getGeneratedClassName(MethodInfo targetMethod, Type handledExceptionType) {
        return targetMethod.declaringClass().name() + "$GeneratedExceptionHandlerFor$"
                + handledExceptionType.name().withoutPackagePrefix()
                + "$OfMethod$" + targetMethod.name();
    }
}
