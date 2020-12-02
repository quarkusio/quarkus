package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.gizmo.MethodDescriptor.*;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.*;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_REQUEST;
import static io.quarkus.resteasy.reactive.server.deployment.GeneratorUtils.paramHandleFromReqContextMethod;
import static io.quarkus.resteasy.reactive.server.deployment.GeneratorUtils.routingContextHandler;
import static io.quarkus.resteasy.reactive.server.deployment.GeneratorUtils.runtimeResourceHandle;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.QUARKUS_REST_CONTAINER_REQUEST_CONTEXT;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.ROUTING_CONTEXT;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.SIMPLIFIED_RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTAINER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HTTP_HEADERS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.URI_INFO;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ContainerRequestContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.HttpHeadersImpl;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.arc.Unremovable;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

final class ServerExceptionMapperGenerator {

    private ServerExceptionMapperGenerator() {
    }

    /**
     * Generates an exception mapper that delegates exception handling to a method of a Resource class.
     * For example, for a method like:
     * 
     * <pre>
     * &#64;ServerExceptionMapper(IllegalArgumentException.class)
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
     *         implements ResteasyReactiveExceptionMapper {
     *
     *     public Response toResponse(IllegalArgumentException e, ServerRequestContext ctx) {
     *         GreetingResource instance = (GreetingResource) ((ResteasyReactiveRequestContext) ctx).getEndpointInstance();
     *         return instance.perClassMapper(e, ctx.getRequest());
     *     }
     * }
     * </pre>
     *
     * @return a map containing the exception class name as the key and the generated exception mapper as the value
     */
    public static Map<String, String> generatePerClassMapper(MethodInfo targetMethod, ClassOutput classOutput) {
        checkReturnType(targetMethod);
        checkModifiers(targetMethod);

        ClassInfo targetClass = targetMethod.declaringClass();
        AnnotationInstance exceptionMapperInstance = targetMethod
                .annotation(SERVER_EXCEPTION_MAPPER);
        Type[] handledExceptionTypes = exceptionMapperInstance.value().asClassArray();
        Map<String, String> result = new HashMap<>();
        for (Type handledExceptionType : handledExceptionTypes) {
            String generatedClassName = getGeneratedClassName(targetMethod, handledExceptionType);
            try (ClassCreator cc = ClassCreator.builder().className(generatedClassName)
                    .interfaces(ResteasyReactiveExceptionMapper.class).classOutput(classOutput).build()) {
                cc.addAnnotation(Singleton.class);
                cc.addAnnotation(Unremovable.class);

                // ctor
                MethodCreator ctor = cc.getMethodCreator("<init>", "V");
                ctor.invokeSpecialMethod(ofConstructor(Object.class), ctor.getThis());
                ctor.returnValue(null);

                MethodDescriptor specToResponseDescriptor = generateSpecToResponse(handledExceptionType, generatedClassName,
                        cc);

                // bridge toResponse(Throwable) method
                generateSpecToResponseBridge(handledExceptionType, cc, specToResponseDescriptor);

                MethodDescriptor rrToResponseDescriptor = ofMethod(generatedClassName, "toResponse",
                        Response.class.getName(),
                        handledExceptionType.name().toString(), ServerRequestContext.class.getName());

                // bridge toResponse(Throwable, ServerRequestContext) method
                generateRRResponseBridge(handledExceptionType, cc, rrToResponseDescriptor);

                // RESTEasy Reactive toResponse(...) method
                generateRRResponse(targetMethod, targetClass, handledExceptionType, cc, rrToResponseDescriptor,
                        (method, contextHandle) -> {
                            ResultHandle endpointInstanceHandle = method.invokeVirtualMethod(
                                    ofMethod(ResteasyReactiveRequestContext.class, "getEndpointInstance", Object.class),
                                    contextHandle);
                            return method.checkCast(endpointInstanceHandle, targetClass.name().toString());
                        });
            }
            result.put(handledExceptionType.name().toString(), generatedClassName);
        }
        return result;
    }

    /**
     * Generates an implementation of {@link ResteasyReactiveExceptionMapper} that delegates to the method
     * annotated with {@code @ServerExceptionMapper}.
     * <p>
     * An example of the generated code is:
     *
     * <pre>
     *
     * &#64;Singleton
     * &#64;Unremovable
     * public class CustomExceptionMapper$GeneratedExceptionHandlerFor$IllegalArgumentException$OfMethod$handle
     *         implements ResteasyReactiveExceptionMapper {
     *     private final CustomExceptionMapper delegate;
     *
     *     &#64;Inject
     *     public CustomExceptionMapper$GeneratedExceptionHandlerFor$IllegalArgumentException$OfMethod$handle(
     *             CustomExceptionMapper var1) {
     *         this.delegate = var1;
     *     }
     *
     *     public Response toResponse(IllegalArgumentException e, ServerRequestContext ctx) {
     *         return delegate.handle(e, ctx.getRequest());
     *     }
     * }
     *
     * </pre>
     *
     * Returns a map containing the handled exception name as the key and the generated class name as the value
     */
    public static Map<String, String> generateGlobalMapper(MethodInfo targetMethod, ClassOutput classOutput) {
        checkReturnType(targetMethod);
        checkModifiers(targetMethod);

        ClassInfo targetClass = targetMethod.declaringClass();
        AnnotationInstance exceptionMapperInstance = targetMethod
                .annotation(SERVER_EXCEPTION_MAPPER);
        Type[] handledExceptionTypes = exceptionMapperInstance.value().asClassArray();
        Map<String, String> result = new HashMap<>();
        for (Type handledExceptionType : handledExceptionTypes) {
            String generatedClassName = getGeneratedClassName(targetMethod, handledExceptionType);
            try (ClassCreator cc = ClassCreator.builder().className(generatedClassName)
                    .interfaces(ResteasyReactiveExceptionMapper.class).classOutput(classOutput).build()) {
                cc.addAnnotation(Singleton.class);
                cc.addAnnotation(Unremovable.class);

                FieldDescriptor delegateField = cc.getFieldCreator("delegate", targetClass.name().toString())
                        .setModifiers(Modifier.PRIVATE | Modifier.FINAL)
                        .getFieldDescriptor();

                // generate a constructor that takes the target class as an argument - this class is a CDI bean so Arc will be able to inject into the generated class
                MethodCreator ctor = cc.getMethodCreator("<init>", void.class, targetClass.name().toString());
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.addAnnotation(Inject.class);
                ctor.invokeSpecialMethod(ofConstructor(Object.class), ctor.getThis());
                ResultHandle self = ctor.getThis();
                ResultHandle config = ctor.getMethodParam(0);
                ctor.writeInstanceField(delegateField, self, config);
                ctor.returnValue(null);

                // spec toResponse(...) method
                MethodDescriptor specToResponseDescriptor = generateSpecToResponse(handledExceptionType, generatedClassName,
                        cc);

                // bridge toResponse(Throwable) method
                generateSpecToResponseBridge(handledExceptionType, cc, specToResponseDescriptor);

                MethodDescriptor rrToResponseDescriptor = ofMethod(generatedClassName, "toResponse",
                        Response.class.getName(),
                        handledExceptionType.name().toString(), ServerRequestContext.class.getName());

                // bridge toResponse(Throwable, ServerRequestContext) method
                generateRRResponseBridge(handledExceptionType, cc, rrToResponseDescriptor);

                // RESTEasy Reactive toResponse(...) method
                generateRRResponse(targetMethod, targetClass, handledExceptionType, cc, rrToResponseDescriptor,
                        (method, contextHandle) -> method.readInstanceField(delegateField, method.getThis()));
            }
            result.put(handledExceptionType.name().toString(), generatedClassName);
        }
        return result;
    }

    private static void generateSpecToResponseBridge(Type handledExceptionType, ClassCreator cc,
            MethodDescriptor specToResponseDescriptor) {
        MethodCreator bridgeSpecToResponse = cc.getMethodCreator("toResponse", Response.class, Throwable.class);
        ResultHandle bridgeSpecParam = bridgeSpecToResponse.getMethodParam(0);
        ResultHandle castedBridgeSpecMethodParam = bridgeSpecToResponse.checkCast(bridgeSpecParam,
                handledExceptionType.name().toString());
        bridgeSpecToResponse.returnValue(bridgeSpecToResponse.invokeVirtualMethod(specToResponseDescriptor,
                bridgeSpecToResponse.getThis(), castedBridgeSpecMethodParam));
    }

    private static MethodDescriptor generateSpecToResponse(Type handledExceptionType, String generatedClassName,
            ClassCreator cc) {
        MethodDescriptor specToResponseDescriptor = ofMethod(generatedClassName, "toResponse",
                Response.class.getName(),
                handledExceptionType.name().toString());
        MethodCreator specToResponse = cc.getMethodCreator(specToResponseDescriptor);
        specToResponse.throwException(IllegalStateException.class, "This should never be called");
        return specToResponseDescriptor;
    }

    private static void generateRRResponseBridge(Type handledExceptionType, ClassCreator cc,
            MethodDescriptor rrToResponseDescriptor) {
        MethodCreator bridgeRRToResponse = cc.getMethodCreator("toResponse", Response.class, Throwable.class,
                ServerRequestContext.class);
        ResultHandle bridgeRRExceptionParam = bridgeRRToResponse.getMethodParam(0);
        ResultHandle bridgeRRContextParam = bridgeRRToResponse.getMethodParam(1);
        ResultHandle castedBridgeQRMethodParam = bridgeRRToResponse.checkCast(bridgeRRExceptionParam,
                handledExceptionType.name().toString());
        bridgeRRToResponse.returnValue(bridgeRRToResponse.invokeVirtualMethod(rrToResponseDescriptor,
                bridgeRRToResponse.getThis(), castedBridgeQRMethodParam, bridgeRRContextParam));
    }

    private static void generateRRResponse(MethodInfo targetMethod, ClassInfo targetClass, Type handledExceptionType,
            ClassCreator cc, MethodDescriptor rrToResponseDescriptor,
            BiFunction<MethodCreator, ResultHandle, ResultHandle> targetInstanceHandleCreator) {
        MethodCreator rrToResponse = cc.getMethodCreator(rrToResponseDescriptor);
        ResultHandle exceptionHandle = rrToResponse.getMethodParam(0);
        ResultHandle contextHandle = rrToResponse.checkCast(rrToResponse.getMethodParam(1),
                ResteasyReactiveRequestContext.class);
        ResultHandle targetInstanceHandle = targetInstanceHandleCreator.apply(rrToResponse, contextHandle);
        if (targetMethod.parameters().isEmpty()) {
            // just call the target method with no parameters
            ResultHandle resultHandle = rrToResponse.invokeVirtualMethod(
                    ofMethod(targetClass.name().toString(), targetMethod.name(), Response.class),
                    targetInstanceHandle);
            rrToResponse.returnValue(resultHandle);
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
                        || QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = rrToResponse.invokeVirtualMethod(
                            ofMethod(ResteasyReactiveRequestContext.class.getName(), "getContainerRequestContext",
                                    ContainerRequestContextImpl.class),
                            contextHandle);
                } else if (URI_INFO.equals(paramDotName)) {
                    paramHandleFromReqContextMethod(rrToResponse, contextHandle, targetMethodParamHandles, i,
                            "getUriInfo",
                            URI_INFO);
                } else if (HTTP_HEADERS.equals(paramDotName)) {
                    paramHandleFromReqContextMethod(rrToResponse, contextHandle, targetMethodParamHandles, i,
                            "getHttpHeaders",
                            HttpHeadersImpl.class);
                } else if (REQUEST.equals(paramDotName)) {
                    paramHandleFromReqContextMethod(rrToResponse, contextHandle, targetMethodParamHandles, i,
                            "getRequest",
                            REQUEST);
                } else if (HTTP_SERVER_REQUEST.equals(paramDotName)) {
                    ResultHandle routingContextHandle = routingContextHandler(rrToResponse, contextHandle);
                    targetMethodParamHandles[i] = rrToResponse.invokeInterfaceMethod(
                            ofMethod(RoutingContext.class, "request", HttpServerRequest.class), routingContextHandle);
                } else if (RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = runtimeResourceHandle(rrToResponse, contextHandle);
                    targetMethodParamHandles[i] = rrToResponse.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getLazyMethod", ResteasyReactiveResourceInfo.class),
                            runtimeResourceHandle);
                } else if (SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = runtimeResourceHandle(rrToResponse, contextHandle);
                    targetMethodParamHandles[i] = rrToResponse.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimpleResourceInfo.class),
                            runtimeResourceHandle);
                } else if (ROUTING_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = routingContextHandler(rrToResponse, contextHandle);
                } else {
                    String parameterName = targetMethod.parameterName(i);
                    throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                            + " of class '" + targetClass.name()
                            + "' is not allowed");
                }
            }

            ResultHandle resultHandle = rrToResponse.invokeVirtualMethod(
                    ofMethod(targetClass.name().toString(), targetMethod.name(),
                            Response.class.getName(), parameterTypes),
                    targetInstanceHandle, targetMethodParamHandles);
            rrToResponse.returnValue(resultHandle);
        }
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
                    + "' cannot be private as it is annotated with '@" + SERVER_EXCEPTION_MAPPER
                    + "'");
        }
        if ((info.flags() & Modifier.STATIC) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be static as it is annotated with '@" + SERVER_EXCEPTION_MAPPER
                    + "'");
        }
    }

    private static String getGeneratedClassName(MethodInfo targetMethod, Type handledExceptionType) {
        return targetMethod.declaringClass().name() + "$GeneratedExceptionHandlerFor$"
                + handledExceptionType.name().withoutPackagePrefix()
                + "$OfMethod$" + targetMethod.name();
    }
}
