package io.quarkus.resteasy.reactive.server.deployment;

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
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.THROWABLE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.URI_INFO;

import java.lang.reflect.Modifier;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.core.QuarkusRestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimplifiedResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestHttpHeaders;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerRequestContext;

import io.quarkus.arc.Unremovable;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Generates the actual implementation of a provider that allows user code using annotations like
 * {@link ServerRequestFilter} and {@link ServerResponseFilter} to work seamlessly
 */
final class CustomProviderGenerator {

    private CustomProviderGenerator() {
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
     *         QuarkusRestRequestContext var2 = (QuarkusRestRequestContext) ((QuarkusRestContainerRequestContext) var1)
     *                 .getQuarkusRestContext();
     *         UriInfo var3 = var2.getUriInfo();
     *         QuarkusRestHttpHeaders var4 = var2.getHttpHeaders();
     *         this.delegate.someMethod(var3, (HttpHeaders) var4);
     *     }
     * }
     *
     * </pre>
     */
    static String generateContainerRequestFilter(MethodInfo targetMethod, ClassOutput classOutput) {
        checkModifiers(targetMethod, SERVER_REQUEST_FILTER);
        String generatedClassName = getGeneratedClassName(targetMethod, SERVER_REQUEST_FILTER);
        DotName declaringClassName = targetMethod.declaringClass().name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(javax.ws.rs.container.ContainerRequestFilter.class.getName())
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

            // generate the implementation of the filter method

            MethodCreator filterMethod = cc.getMethodCreator("filter", void.class, ContainerRequestContext.class);
            ResultHandle qrReqCtxHandle = getQRReqCtxHandle(filterMethod, 0);

            // for each of the parameters of the user method, generate bytecode that pulls the argument outs of QuarkusRestRequestContext
            ResultHandle[] targetMethodParamHandles = new ResultHandle[targetMethod.parameters().size()];
            for (int i = 0; i < targetMethod.parameters().size(); i++) {
                Type param = targetMethod.parameters().get(i);
                DotName paramDotName = param.name();
                if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = filterMethod.getMethodParam(0);
                } else if (QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = filterMethod.checkCast(filterMethod.getMethodParam(0),
                            QuarkusRestContainerRequestContext.class);
                } else if (URI_INFO.equals(paramDotName)) {
                    GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, qrReqCtxHandle, targetMethodParamHandles, i,
                            "getUriInfo",
                            URI_INFO);
                } else if (HTTP_HEADERS.equals(paramDotName)) {
                    GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, qrReqCtxHandle, targetMethodParamHandles, i,
                            "getHttpHeaders",
                            QuarkusRestHttpHeaders.class);
                } else if (REQUEST.equals(paramDotName)) {
                    GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, qrReqCtxHandle, targetMethodParamHandles, i,
                            "getRequest",
                            REQUEST);
                } else if (HTTP_SERVER_REQUEST.equals(paramDotName)) {
                    ResultHandle routingContextHandle = GeneratorUtils.routingContextHandler(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeInterfaceMethod(
                            ofMethod(RoutingContext.class, "request", HttpServerRequest.class), routingContextHandle);
                } else if (RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getLazyMethod", LazyMethod.class), runtimeResourceHandle);
                } else if (SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimplifiedResourceInfo.class),
                            runtimeResourceHandle);
                } else if (ROUTING_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = GeneratorUtils.routingContextHandler(filterMethod, qrReqCtxHandle);
                } else {
                    String parameterName = targetMethod.parameterName(i);
                    throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                            + " of class '" + declaringClassName
                            + "' is not allowed");
                }
            }
            // call the target method
            filterMethod.invokeVirtualMethod(targetMethod,
                    filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                    targetMethodParamHandles);
            filterMethod.returnValue(null);
        }
        return generatedClassName;
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
     *         QuarkusRestRequestContext var3 = (QuarkusRestRequestContext) ((QuarkusRestContainerRequestContext) var1)
     *                 .getQuarkusRestContext();
     *         UriInfo var4 = var2.getUriInfo();
     *         this.delegate.someMethod(var4);
     *     }
     * }
     *
     * </pre>
     */
    static String generateContainerResponseFilter(MethodInfo targetMethod, ClassOutput classOutput) {
        checkModifiers(targetMethod, SERVER_RESPONSE_FILTER);
        String generatedClassName = getGeneratedClassName(targetMethod, SERVER_RESPONSE_FILTER);
        DotName declaringClassName = targetMethod.declaringClass().name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(javax.ws.rs.container.ContainerResponseFilter.class.getName())
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

            // generate the implementation of the filter method

            MethodCreator filterMethod = cc.getMethodCreator("filter", void.class, ContainerRequestContext.class,
                    ContainerResponseContext.class);
            ResultHandle qrReqCtxHandle = getQRReqCtxHandle(filterMethod, 0);

            // TODO: should we add any more things here? Vert.x response could be one but it seems risky...
            // for each of the parameters of the user method, generate bytecode that pulls the arguments from the proper places
            ResultHandle[] targetMethodParamHandles = new ResultHandle[targetMethod.parameters().size()];
            for (int i = 0; i < targetMethod.parameters().size(); i++) {
                Type param = targetMethod.parameters().get(i);
                DotName paramDotName = param.name();
                if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = filterMethod.getMethodParam(0);
                } else if (QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = filterMethod.checkCast(filterMethod.getMethodParam(0),
                            QuarkusRestContainerRequestContext.class);
                } else if (CONTAINER_RESPONSE_CONTEXT.equals(paramDotName)) {
                    targetMethodParamHandles[i] = filterMethod.getMethodParam(1);
                } else if (HTTP_SERVER_REQUEST.equals(paramDotName)) {
                    ResultHandle routingContextHandle = GeneratorUtils.routingContextHandler(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeInterfaceMethod(
                            ofMethod(RoutingContext.class, "request", HttpServerRequest.class), routingContextHandle);
                } else if (HTTP_SERVER_RESPONSE.equals(paramDotName)) {
                    ResultHandle routingContextHandle = GeneratorUtils.routingContextHandler(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeInterfaceMethod(
                            ofMethod(RoutingContext.class, "response", HttpServerResponse.class), routingContextHandle);
                } else if (RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getLazyMethod", LazyMethod.class), runtimeResourceHandle);
                } else if (SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                    ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, qrReqCtxHandle);
                    targetMethodParamHandles[i] = filterMethod.invokeVirtualMethod(
                            ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimplifiedResourceInfo.class),
                            runtimeResourceHandle);
                } else if (THROWABLE.equals(paramDotName)) {
                    GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, qrReqCtxHandle, targetMethodParamHandles, i,
                            "getThrowable",
                            THROWABLE);
                } else {
                    String parameterName = targetMethod.parameterName(i);
                    throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                            + " of class '" + declaringClassName
                            + "' is not allowed");
                }
            }
            // call the target method
            filterMethod.invokeVirtualMethod(targetMethod,
                    filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                    targetMethodParamHandles);
            filterMethod.returnValue(null);
        }
        return generatedClassName;
    }

    private static ResultHandle getQRReqCtxHandle(MethodCreator filter, int containerReqCtxParamIndex) {
        ResultHandle containerReqCtxHandle = filter.getMethodParam(containerReqCtxParamIndex);
        ResultHandle qrContainerReqCtxHandle = filter.checkCast(containerReqCtxHandle,
                QuarkusRestContainerRequestContext.class);
        ResultHandle qrCtxHandle = filter.invokeInterfaceMethod(
                ofMethod(QuarkusRestContainerRequestContext.class, "getQuarkusRestContext", QuarkusRestContext.class),
                qrContainerReqCtxHandle);
        return filter.checkCast(qrCtxHandle, ResteasyReactiveRequestContext.class);
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
}
