package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_REQUEST;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_RESPONSE;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.QUARKUS_REST_CONTAINER_REQUEST_CONTEXT;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.ROUTING_CONTEXT;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.SERVER_REQUEST_FILTER;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.SERVER_RESPONSE_FILTER;
import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveServerDotNames.SIMPLIFIED_RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTAINER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTAINER_RESPONSE_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HTTP_HEADERS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONAL;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.THROWABLE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.URI_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.VOID;

import java.lang.reflect.Modifier;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.HttpHeadersImpl;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.arc.Unremovable;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.server.runtime.filters.FilterUtil;
import io.quarkus.resteasy.reactive.server.runtime.filters.PreventAbortResteasyReactiveContainerRequestContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Generates the actual implementation of a provider that allows user code using annotations like
 * {@link ServerRequestFilter} and {@link ServerResponseFilter} to work seamlessly
 */
final class CustomFilterGenerator {

    private CustomFilterGenerator() {
    }

    /**
     * Generates an implementation of {@link javax.ws.rs.container.ContainerRequestFilter} that delegates to the method
     * annotated with {@code @ServerRequestFilter}.
     * <p>
     * An example of the generated code is:
     *
     * <pre>
     *
     * &#64;Singleton
     * &#64;Unremovable
     * public class CustomContainerRequestFilter$GeneratedContainerRequestFilter$someMethod implements ServerRequestFilter {
     *     private final CustomContainerRequestFilter delegate;
     *
     *     &#64;Inject
     *     public CustomContainerRequestFilter$GeneratedContainerRequestFilter$someMethod(CustomContainerRequestFilter var1) {
     *         this.delegate = var1;
     *     }
     *
     *     public void filter(ContainerRequestContext var1) {
     *         QuarkusRestRequestContext var2 = (QuarkusRestRequestContext) ((ResteasyReactiveContainerRequestContext) var1)
     *                 .getQuarkusRestContext();
     *         UriInfo var3 = var2.getUriInfo();
     *         HttpHeadersImpl var4 = var2.getHttpHeaders();
     *         this.delegate.someMethod(var3, (HttpHeaders) var4);
     *     }
     * }
     *
     * </pre>
     */
    static String generateContainerRequestFilter(MethodInfo targetMethod, ClassOutput classOutput) {
        ReturnType returnType = determineRequestFilterReturnType(targetMethod);
        checkModifiers(targetMethod, SERVER_REQUEST_FILTER);
        String generatedClassName = getGeneratedClassName(targetMethod, SERVER_REQUEST_FILTER);
        DotName declaringClassName = targetMethod.declaringClass().name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(determineRequestInterfaceType(returnType))
                .build()) {
            cc.addAnnotation(Singleton.class);
            cc.addAnnotation(Unremovable.class);

            FieldDescriptor delegateField = cc.getFieldCreator("delegate", declaringClassName.toString())
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL)
                    .getFieldDescriptor();

            // generate a constructor that takes the target class as an argument - this class is a CDI bean so Arc will be able to inject into the generated class
            MethodCreator ctor = cc.getMethodCreator("<init>", void.class, declaringClassName.toString());
            ctor.setModifiers(Modifier.PUBLIC);
            ctor.addAnnotation(Inject.class);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
            ResultHandle self = ctor.getThis();
            ResultHandle config = ctor.getMethodParam(0);
            ctor.writeInstanceField(delegateField, self, config);
            ctor.returnValue(null);

            if ((returnType == ReturnType.VOID) || (returnType == ReturnType.OPTIONAL_RESPONSE)
                    || (returnType == ReturnType.RESPONSE)) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class, ContainerRequestContext.class);
                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                // call the target method
                ResultHandle resultHandle = filterMethod.invokeVirtualMethod(targetMethod,
                        filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                        getRequestFilterResultHandles(targetMethod, declaringClassName, filterMethod,
                                getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle)));
                if (returnType == ReturnType.OPTIONAL_RESPONSE) {
                    // invoke utility class that deals with Optional
                    filterMethod.invokeStaticMethod(ofMethod(FilterUtil.class, "handleOptional", void.class,
                            Optional.class, ResteasyReactiveContainerRequestContext.class), resultHandle,
                            rrContainerReqCtxHandle);
                } else if (returnType == ReturnType.RESPONSE) {
                    filterMethod.invokeStaticMethod(ofMethod(FilterUtil.class, "handleResponse", void.class,
                            Response.class, ResteasyReactiveContainerRequestContext.class), resultHandle,
                            rrContainerReqCtxHandle);
                }
                filterMethod.returnValue(null);
            } else if ((returnType == ReturnType.UNI_VOID) || (returnType == ReturnType.UNI_RESPONSE)) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class,
                        ResteasyReactiveContainerRequestContext.class);
                // call the target method
                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                ResultHandle uniHandle = filterMethod.invokeVirtualMethod(targetMethod,
                        filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                        getRequestFilterResultHandles(targetMethod, declaringClassName, filterMethod,
                                getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle)));
                // invoke utility class that deals with suspend / resume
                filterMethod
                        .invokeStaticMethod(
                                ofMethod(FilterUtil.class,
                                        returnType == ReturnType.UNI_VOID ? "handleUniVoid"
                                                : "handleUniResponse",
                                        void.class,
                                        Uni.class, ResteasyReactiveContainerRequestContext.class),
                                uniHandle, rrContainerReqCtxHandle);
                filterMethod.returnValue(null);
            } else {
                throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
            }
        }
        return generatedClassName;
    }

    private static ResultHandle[] getRequestFilterResultHandles(MethodInfo targetMethod, DotName declaringClassName,
            MethodCreator filterMethod, ResultHandle rrReqCtxHandle) {
        // for each of the parameters of the user method, generate bytecode that pulls the argument outs of QuarkusRestRequestContext
        ResultHandle[] targetMethodParamHandles = new ResultHandle[targetMethod.parameters().size()];
        for (int i = 0; i < targetMethod.parameters().size(); i++) {
            Type param = targetMethod.parameters().get(i);
            DotName paramDotName = param.name();
            if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.newInstance(
                        ofConstructor(PreventAbortResteasyReactiveContainerRequestContext.class, ContainerRequestContext.class),
                        filterMethod.getMethodParam(0));
                ;
            } else if (QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.checkCast(filterMethod.getMethodParam(0),
                        ResteasyReactiveContainerRequestContext.class);
            } else if (URI_INFO.equals(paramDotName)) {
                GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, rrReqCtxHandle, targetMethodParamHandles,
                        i,
                        "getUriInfo",
                        URI_INFO);
            } else if (HTTP_HEADERS.equals(paramDotName)) {
                GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, rrReqCtxHandle, targetMethodParamHandles,
                        i,
                        "getHttpHeaders",
                        HttpHeadersImpl.class);
            } else if (REQUEST.equals(paramDotName)) {
                GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, rrReqCtxHandle, targetMethodParamHandles,
                        i,
                        "getRequest",
                        REQUEST);
            } else if (HTTP_SERVER_REQUEST.equals(paramDotName)) {
                ResultHandle routingContextHandle = GeneratorUtils.routingContextHandler(filterMethod, rrReqCtxHandle);
                targetMethodParamHandles[i] = filterMethod.invokeInterfaceMethod(
                        ofMethod(RoutingContext.class, "request", HttpServerRequest.class), routingContextHandle);
            } else if (RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getSimpleResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (ROUTING_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = GeneratorUtils.routingContextHandler(filterMethod, rrReqCtxHandle);
            } else {
                String parameterName = targetMethod.parameterName(i);
                throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                        + " of class '" + declaringClassName
                        + "' is not allowed");
            }
        }
        return targetMethodParamHandles;
    }

    /**
     * Generates an implementation of {@link javax.ws.rs.container.ContainerResponseFilter} that delegates to the method
     * annotated with {@code @ServerResponseFilter}.
     * <p>
     * An example of the generated code is:
     *
     * <pre>
     *
     * &#64;Singleton
     * &#64;Unremovable
     * public class CustomContainerResponseFilter$GeneratedContainerResponseFilter$someMethod
     *         implements ServerResponseFilter {
     *     private final CustomContainerRequestFilter delegate;
     *
     *     &#64;Inject
     *     public CustomContainerResponseFilter$GeneratedContainerResponseFilter$someMethod(CustomContainerRequestFilter var1) {
     *         this.delegate = var1;
     *     }
     *
     *     public void filter(ContainerRequestContext var1, ContainerResponseContext var2) {
     *         QuarkusRestRequestContext var3 = (QuarkusRestRequestContext) ((ResteasyReactiveContainerRequestContext) var1)
     *                 .getQuarkusRestContext();
     *         UriInfo var4 = var2.getUriInfo();
     *         this.delegate.someMethod(var4);
     *     }
     * }
     *
     * </pre>
     */
    static String generateContainerResponseFilter(MethodInfo targetMethod, ClassOutput classOutput) {
        ReturnType returnType = determineResponseFilterReturnType(targetMethod);
        checkModifiers(targetMethod, SERVER_RESPONSE_FILTER);
        String generatedClassName = getGeneratedClassName(targetMethod, SERVER_RESPONSE_FILTER);
        DotName declaringClassName = targetMethod.declaringClass().name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(determineResponseInterfaceType(returnType))
                .build()) {
            cc.addAnnotation(Singleton.class);
            cc.addAnnotation(Unremovable.class);

            FieldDescriptor delegateField = cc.getFieldCreator("delegate", declaringClassName.toString())
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL)
                    .getFieldDescriptor();

            // generate a constructor that takes the target class as an argument - this class is a CDI bean so Arc will be able to inject into the generated class
            MethodCreator ctor = cc.getMethodCreator("<init>", void.class, declaringClassName.toString());
            ctor.setModifiers(Modifier.PUBLIC);
            ctor.addAnnotation(Inject.class);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
            ResultHandle self = ctor.getThis();
            ResultHandle config = ctor.getMethodParam(0);
            ctor.writeInstanceField(delegateField, self, config);
            ctor.returnValue(null);

            if (returnType == ReturnType.VOID) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class, ContainerRequestContext.class,
                        ContainerResponseContext.class);

                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                ResultHandle rrReqCtxHandle = getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle);

                // call the target method
                filterMethod.invokeVirtualMethod(targetMethod,
                        filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                        getResponseFilterResultHandles(targetMethod, declaringClassName,
                                filterMethod, rrReqCtxHandle));
                filterMethod.returnValue(null);
            } else if (returnType == ReturnType.UNI_VOID) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class,
                        ResteasyReactiveContainerRequestContext.class,
                        ContainerResponseContext.class);

                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                ResultHandle rrReqCtxHandle = getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle);

                // call the target method
                ResultHandle uniHandle = filterMethod.invokeVirtualMethod(targetMethod,
                        filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                        getResponseFilterResultHandles(targetMethod, declaringClassName,
                                filterMethod, rrReqCtxHandle));

                // invoke utility class that deals with Optional
                filterMethod.invokeStaticMethod(
                        ofMethod(FilterUtil.class, "handleUniVoid", void.class,
                                Uni.class, ResteasyReactiveContainerRequestContext.class),
                        uniHandle, rrContainerReqCtxHandle);
                filterMethod.returnValue(null);
            }
        }
        return generatedClassName;
    }

    private static ResultHandle[] getResponseFilterResultHandles(MethodInfo targetMethod, DotName declaringClassName,
            MethodCreator filterMethod, ResultHandle rrReqCtxHandle) {
        ResultHandle[] targetMethodParamHandles = new ResultHandle[targetMethod.parameters().size()];
        for (int i = 0; i < targetMethod.parameters().size(); i++) {
            Type param = targetMethod.parameters().get(i);
            DotName paramDotName = param.name();
            if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.newInstance(
                        ofConstructor(PreventAbortResteasyReactiveContainerRequestContext.class,
                                ContainerRequestContext.class),
                        filterMethod.getMethodParam(0));
            } else if (QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.checkCast(filterMethod.getMethodParam(0),
                        ResteasyReactiveContainerRequestContext.class);
            } else if (CONTAINER_RESPONSE_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.getMethodParam(1);
            } else if (HTTP_SERVER_REQUEST.equals(paramDotName)) {
                ResultHandle routingContextHandle = GeneratorUtils.routingContextHandler(filterMethod, rrReqCtxHandle);
                targetMethodParamHandles[i] = filterMethod.invokeInterfaceMethod(
                        ofMethod(RoutingContext.class, "request", HttpServerRequest.class), routingContextHandle);
            } else if (HTTP_SERVER_RESPONSE.equals(paramDotName)) {
                ResultHandle routingContextHandle = GeneratorUtils.routingContextHandler(filterMethod, rrReqCtxHandle);
                targetMethodParamHandles[i] = filterMethod.invokeInterfaceMethod(
                        ofMethod(RoutingContext.class, "response", HttpServerResponse.class), routingContextHandle);
            } else if (RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getSimpleResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (THROWABLE.equals(paramDotName)) {
                GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, rrReqCtxHandle, targetMethodParamHandles, i,
                        "getThrowable",
                        THROWABLE);
            } else {
                String parameterName = targetMethod.parameterName(i);
                throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                        + " of class '" + declaringClassName
                        + "' is not allowed");
            }
        }
        return targetMethodParamHandles;
    }

    private static ResultHandle getRRContainerReqCtxHandle(MethodCreator filter, int containerReqCtxParamIndex) {
        ResultHandle containerReqCtxHandle = filter.getMethodParam(containerReqCtxParamIndex);
        return filter.checkCast(containerReqCtxHandle,
                ResteasyReactiveContainerRequestContext.class);
    }

    private static ResultHandle getRRReqCtxHandle(MethodCreator filter, ResultHandle rrContainerReqCtxHandle) {
        ResultHandle rrCtxHandle = filter.invokeInterfaceMethod(
                ofMethod(ResteasyReactiveContainerRequestContext.class, "getServerRequestContext",
                        ServerRequestContext.class),
                rrContainerReqCtxHandle);
        return filter.checkCast(rrCtxHandle, ResteasyReactiveRequestContext.class);
    }

    private static AssignableResultHandle getResourceInfoHandle(MethodCreator filterMethod, ResultHandle rrReqCtxHandle) {
        ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, rrReqCtxHandle);
        AssignableResultHandle resourceInfo = filterMethod.createVariable(ResteasyReactiveResourceInfo.class);
        BranchResult ifNullBranch = filterMethod.ifNull(runtimeResourceHandle);
        ifNullBranch.trueBranch().assign(resourceInfo, ifNullBranch.trueBranch().readStaticField(
                FieldDescriptor.of(SimpleResourceInfo.NullValues.class, "INSTANCE", SimpleResourceInfo.NullValues.class)));
        ifNullBranch.falseBranch().assign(resourceInfo, ifNullBranch.falseBranch().invokeVirtualMethod(
                ofMethod(RuntimeResource.class, "getLazyMethod", ResteasyReactiveResourceInfo.class),
                runtimeResourceHandle));
        return resourceInfo;
    }

    private static AssignableResultHandle getSimpleResourceInfoHandle(MethodCreator filterMethod, ResultHandle rrReqCtxHandle) {
        ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, rrReqCtxHandle);
        AssignableResultHandle resourceInfo = filterMethod.createVariable(SimpleResourceInfo.class);
        BranchResult ifNullBranch = filterMethod.ifNull(runtimeResourceHandle);
        ifNullBranch.trueBranch().assign(resourceInfo, ifNullBranch.trueBranch().readStaticField(
                FieldDescriptor.of(SimpleResourceInfo.NullValues.class, "INSTANCE", SimpleResourceInfo.NullValues.class)));
        ifNullBranch.falseBranch().assign(resourceInfo, ifNullBranch.falseBranch().invokeVirtualMethod(
                ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimpleResourceInfo.class),
                runtimeResourceHandle));
        return resourceInfo;
    }

    private static String getGeneratedClassName(MethodInfo targetMethod, DotName annotationDotName) {
        DotName declaringClassName = targetMethod.declaringClass().name();
        return declaringClassName.toString() + "$Generated" + annotationDotName.withoutPackagePrefix() + "$"
                + targetMethod.name();
    }

    private static void checkModifiers(MethodInfo info, DotName annotationDotName) {
        if ((info.flags() & Modifier.PRIVATE) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be private as it is annotated with '@" + annotationDotName + "'");
        }
        if ((info.flags() & Modifier.STATIC) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be static as it is annotated with '@" + annotationDotName + "'");
        }
    }

    private static ReturnType determineRequestFilterReturnType(MethodInfo targetMethod) {
        if (targetMethod.returnType().kind() == Type.Kind.VOID) {
            return ReturnType.VOID;
        } else if (targetMethod.returnType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = targetMethod.returnType().asParameterizedType();
            if (parameterizedType.name().equals(UNI) && (parameterizedType.arguments().size() == 1)) {
                if (parameterizedType.arguments().get(0).name().equals(VOID)) {
                    return ReturnType.UNI_VOID;
                } else if (parameterizedType.arguments().get(0).name().equals(RESPONSE)) {
                    return ReturnType.UNI_RESPONSE;
                }
            } else if (parameterizedType.name().equals(OPTIONAL) && (parameterizedType.arguments().size() == 1)) {
                if (parameterizedType.arguments().get(0).name().equals(RESPONSE)) {
                    return ReturnType.OPTIONAL_RESPONSE;
                }
            }
        } else if (targetMethod.returnType().name().equals(RESPONSE)) {
            return ReturnType.RESPONSE;
        }
        throw new RuntimeException("Method '" + targetMethod.name() + " of class '" + targetMethod.declaringClass().name()
                + "' cannot be used as a request filter as it does not declare 'void', Optional<Response>, 'Uni<Void>' or 'Uni<Response>' as its return type");
    }

    private static ReturnType determineResponseFilterReturnType(MethodInfo targetMethod) {
        if (targetMethod.returnType().kind() == Type.Kind.VOID) {
            return ReturnType.VOID;
        } else if (targetMethod.returnType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = targetMethod.returnType().asParameterizedType();
            if (parameterizedType.name().equals(UNI) && (parameterizedType.arguments().size() == 1)) {
                if (parameterizedType.arguments().get(0).name().equals(VOID)) {
                    return ReturnType.UNI_VOID;
                }
            }
        }
        throw new RuntimeException("Method '" + targetMethod.name() + " of class '" + targetMethod.declaringClass().name()
                + "' cannot be used as a response filter as it does not declare 'void' or 'Uni<Void>' as its return type");
    }

    private static Class<?> determineRequestInterfaceType(ReturnType returnType) {
        if ((returnType == ReturnType.VOID) || (returnType == ReturnType.OPTIONAL_RESPONSE)
                || (returnType == ReturnType.RESPONSE)) {
            return ContainerRequestFilter.class;
        } else if ((returnType == ReturnType.UNI_VOID) || (returnType == ReturnType.UNI_RESPONSE)) {
            return ResteasyReactiveContainerRequestFilter.class;
        }
        throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
    }

    private static Class<?> determineResponseInterfaceType(ReturnType returnType) {
        if (returnType == ReturnType.VOID) {
            return ContainerResponseFilter.class;
        } else if (returnType == ReturnType.UNI_VOID) {
            return ResteasyReactiveContainerResponseFilter.class;
        }
        throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
    }

    private enum ReturnType {
        VOID,
        RESPONSE,
        OPTIONAL_RESPONSE,
        UNI_VOID,
        UNI_RESPONSE
    }
}
