package org.jboss.resteasy.reactive.server.processor.generation.exceptionmappers;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTAINER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HTTP_HEADERS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.SERVER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.URI_INFO;
import static org.jboss.resteasy.reactive.server.processor.generation.multipart.GeneratorUtils.paramHandleFromReqContextMethod;
import static org.jboss.resteasy.reactive.server.processor.generation.multipart.GeneratorUtils.runtimeResourceHandle;
import static org.jboss.resteasy.reactive.server.processor.generation.multipart.GeneratorUtils.unwrapObject;
import static org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames.QUARKUS_REST_CONTAINER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames.SIMPLIFIED_RESOURCE_INFO;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.processor.HashUtil;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.exceptionmappers.AsyncExceptionMappingUtil;
import org.jboss.resteasy.reactive.server.jaxrs.ContainerRequestContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.HttpHeadersImpl;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.AsyncExceptionMapperContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveAsyncExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.smallrye.mutiny.Uni;

public final class ServerExceptionMapperGenerator {

    private static final DotName THROWABLE = DotName.createSimple(Throwable.class.getName());
    private static final DotName EXCEPTION = DotName.createSimple(Exception.class.getName());

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
     * <p>
     * a generated exception mapper would look like this (only the actual called method is displayed)
     *
     * <pre>
     * &#64;Singleton
     * &#64;Unremovable
     * public class GreetingResource$ExceptionMapper$123456
     *         implements ResteasyReactiveExceptionMapper {
     *
     *     public Response toResponse(IllegalArgumentException e, ServerRequestContext ctx) {
     *         GreetingResource instance = (GreetingResource) ((ResteasyReactiveRequestContext) ctx).getEndpointInstance();
     *         return instance.perClassMapper(e, ctx.getRequest());
     *     }
     * }
     * </pre>
     * <p>
     * An example of a generated exception handler that returns Uni is:
     *
     * <pre>
     * &#64;Singleton
     * &#64;Unremovable
     * public class GreetingResource$ExceptionMapper$123456
     *         implements ResteasyReactiveAsyncExceptionMapper {
     *
     *     public void asyncResponse(IllegalArgumentException e, AsyncExceptionMapperContext ctx) {
     *         GreetingResource instance = (GreetingResource) ((ResteasyReactiveRequestContext) ctx).getEndpointInstance();
     *         AsyncExceptionMappingUtil.handleUniResponse(instance.perClassMapper(e, ctx.getRequest()));
     *     }
     * }
     * </pre>
     *
     * @return a map containing the exception class name as the key and the generated exception mapper as the value
     */
    public static Map<String, String> generatePerClassMapper(MethodInfo targetMethod, ClassOutput classOutput,
            Set<DotName> unwrappableTypes, Set<String> additionalBeanAnnotations) {
        ReturnType returnType = determineReturnType(targetMethod);
        checkModifiers(targetMethod);

        ClassInfo targetClass = targetMethod.declaringClass();
        Type[] handledExceptionTypes = getHandledExceptionTypes(targetMethod);
        Map<String, String> result = new HashMap<>();

        boolean handlesMultipleExceptions = handledExceptionTypes.length > 1;
        Set<String> commonHierarchyOfExceptions = getCommonHierarchyOfExceptions(handledExceptionTypes);

        for (Type handledExceptionType : handledExceptionTypes) {
            String generatedClassName = getGeneratedClassName(targetMethod, handledExceptionType);
            try (ClassCreator cc = ClassCreator.builder().className(generatedClassName)
                    .interfaces(determineInterfaceType(returnType)).classOutput(classOutput).build()) {
                cc.addAnnotation(Singleton.class);
                for (var an : additionalBeanAnnotations) {
                    cc.addAnnotation(an);
                }

                // ctor
                MethodCreator ctor = cc.getMethodCreator("<init>", "V");
                ctor.invokeSpecialMethod(ofConstructor(Object.class), ctor.getThis());
                ctor.returnValue(null);

                if (returnType == ReturnType.RESPONSE || returnType == ReturnType.REST_RESPONSE) {
                    MethodDescriptor specToResponseDescriptor = generateSpecToResponse(handledExceptionType, generatedClassName,
                            cc);

                    // bridge toResponse(Throwable) method
                    generateSpecToResponseBridge(handledExceptionType, cc, specToResponseDescriptor);

                    MethodDescriptor rrToResponseDescriptor = toResponseDescriptor(handledExceptionType, generatedClassName);

                    // bridge toResponse(Throwable, ServerRequestContext) method
                    if (!THROWABLE.equals(handledExceptionType.name())) {
                        generateRRResponseBridge(handledExceptionType, cc, rrToResponseDescriptor);
                    }

                    // RESTEasy Reactive toResponse(...) method
                    generateRRResponse(targetMethod, targetClass, handledExceptionType, cc, rrToResponseDescriptor,
                            returnType, handlesMultipleExceptions, commonHierarchyOfExceptions,
                            (method, contextHandle) -> {
                                ResultHandle endpointInstanceHandle = method.invokeVirtualMethod(
                                        ofMethod(ResteasyReactiveRequestContext.class, "getEndpointInstance", Object.class),
                                        contextHandle);
                                return method.checkCast(endpointInstanceHandle, targetClass.name().toString());
                            }, unwrappableTypes);
                } else if (returnType == ReturnType.UNI_RESPONSE || returnType == ReturnType.UNI_REST_RESPONSE) {
                    MethodDescriptor rrAsyncResponseDescriptor = asyncResponseDescriptor(handledExceptionType,
                            generatedClassName);

                    // bridge asyncResponse(Throwable, AsyncExceptionMapperContext) method
                    if (!THROWABLE.equals(handledExceptionType.name())) {
                        generateRRUniResponseBridge(handledExceptionType, cc, rrAsyncResponseDescriptor);
                    }

                    // RESTEasy Reactive asyncResponse(...) method
                    generateRRUniResponse(targetMethod, targetClass, handledExceptionType, cc, rrAsyncResponseDescriptor,
                            returnType, handlesMultipleExceptions, commonHierarchyOfExceptions,
                            (method, contextHandle) -> {
                                ResultHandle endpointInstanceHandle = method.invokeVirtualMethod(
                                        ofMethod(ResteasyReactiveRequestContext.class, "getEndpointInstance", Object.class),
                                        contextHandle);
                                return method.checkCast(endpointInstanceHandle, targetClass.name().toString());
                            }, unwrappableTypes);
                } else {
                    throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
                }
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
     * public class CustomExceptionMapper$ExceptionMapper$123456
     *         implements ResteasyReactiveExceptionMapper {
     *     private final CustomExceptionMapper delegate;
     *
     *     &#64;Inject
     *     public CustomExceptionMapper$ExceptionMapper$123456(
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
     * <p>
     * An example of a generated exception handler that returns Uni is:
     *
     * <pre>
     *
     * &#64;Singleton
     * &#64;Unremovable
     * public class CustomExceptionMapper$ExceptionMapper$123456
     *         implements ResteasyReactiveAsyncExceptionMapper {
     *     private final CustomExceptionMapper delegate;
     *
     *     &#64;Inject
     *     public CustomExceptionMapper$ExceptionMapper$123456(
     *             CustomExceptionMapper var1) {
     *         this.delegate = var1;
     *     }
     *
     *     public void asyncResponse(IllegalArgumentException e, AsyncExceptionMapperContext ctx) {
     *         AsyncExceptionMappingUtil.handleUniResponse(delegate.handle(e, ctx.getRequest()));
     *     }
     * }
     *
     * </pre>
     * <p>
     * Returns a map containing the handled exception name as the key and the generated class name as the value
     */
    public static Map<String, String> generateGlobalMapper(MethodInfo targetMethod, ClassOutput classOutput,
            Set<DotName> unwrappableTypes, Set<String> additionalBeanAnnotations, Predicate<MethodInfo> isOptionalMapper) {
        ReturnType returnType = determineReturnType(targetMethod);
        checkModifiers(targetMethod);

        ClassInfo targetClass = targetMethod.declaringClass();
        Type[] handledExceptionTypes = getHandledExceptionTypes(targetMethod);
        Map<String, String> result = new HashMap<>();

        boolean handlesMultipleExceptions = handledExceptionTypes.length > 1;
        Set<String> commonHierarchyOfExceptions = getCommonHierarchyOfExceptions(handledExceptionTypes);

        for (Type handledExceptionType : handledExceptionTypes) {
            String generatedClassName = getGeneratedClassName(targetMethod, handledExceptionType);
            try (ClassCreator cc = ClassCreator.builder().className(generatedClassName)
                    .interfaces(determineInterfaceType(returnType)).classOutput(classOutput).build()) {
                cc.addAnnotation(Singleton.class);
                for (var an : additionalBeanAnnotations) {
                    cc.addAnnotation(an);
                }

                FieldDescriptor delegateField = cc.getFieldCreator("delegate", targetClass.name().toString())
                        .setModifiers(Modifier.PRIVATE | Modifier.FINAL)
                        .getFieldDescriptor();

                MethodCreator ctor;

                boolean isOptional = isOptionalMapper.test(targetMethod);
                if (isOptional) {
                    // generate a constructor that takes the Instance<TargetClass> as an argument in order to avoid missing bean issues if the target has been conditionally disabled
                    // the body can freely read the instance value because if the target has been conditionally disabled, the generated class will not be instantiated
                    ctor = cc.getMethodCreator("<init>", void.class, Instance.class).setSignature(
                            String.format("(L%s<L%s;>;)V",
                                    Instance.class.getName().replace('.', '/'),
                                    targetClass.name().toString().replace('.', '/')));
                } else {
                    // generate a constructor that takes the target class as an argument - this class is a CDI bean so Arc will be able to inject into the generated class
                    ctor = cc.getMethodCreator("<init>", void.class, targetClass.name().toString());
                }

                ctor.setModifiers(Modifier.PUBLIC);
                ctor.addAnnotation(Inject.class);
                ctor.invokeSpecialMethod(ofConstructor(Object.class), ctor.getThis());
                ResultHandle self = ctor.getThis();
                ResultHandle param = ctor.getMethodParam(0);
                if (isOptional) {
                    ctor.writeInstanceField(delegateField, self, ctor
                            .invokeInterfaceMethod(MethodDescriptor.ofMethod(Instance.class, "get", Object.class), param));
                } else {
                    ctor.writeInstanceField(delegateField, self, param);
                }
                ctor.returnValue(null);

                if (returnType == ReturnType.RESPONSE || returnType == ReturnType.REST_RESPONSE) {
                    // spec toResponse(...) method
                    MethodDescriptor specToResponseDescriptor = generateSpecToResponse(handledExceptionType, generatedClassName,
                            cc);

                    // bridge toResponse(Throwable) method
                    generateSpecToResponseBridge(handledExceptionType, cc, specToResponseDescriptor);

                    MethodDescriptor rrToResponseDescriptor = toResponseDescriptor(handledExceptionType, generatedClassName);

                    // bridge toResponse(Throwable, ServerRequestContext) method
                    if (!THROWABLE.equals(handledExceptionType.name())) {
                        generateRRResponseBridge(handledExceptionType, cc, rrToResponseDescriptor);
                    }

                    // RESTEasy Reactive toResponse(...) method
                    generateRRResponse(targetMethod, targetClass, handledExceptionType, cc, rrToResponseDescriptor,
                            returnType, handlesMultipleExceptions, commonHierarchyOfExceptions,
                            (method, contextHandle) -> method.readInstanceField(delegateField, method.getThis()),
                            unwrappableTypes);
                } else if (returnType == ReturnType.UNI_RESPONSE || returnType == ReturnType.UNI_REST_RESPONSE) {
                    MethodDescriptor rrAsyncResponseDescriptor = asyncResponseDescriptor(handledExceptionType,
                            generatedClassName);

                    // bridge asyncResponse(Throwable, AsyncExceptionMapperContext) method
                    if (!THROWABLE.equals(handledExceptionType.name())) {
                        generateRRUniResponseBridge(handledExceptionType, cc, rrAsyncResponseDescriptor);
                    }

                    // RESTEasy Reactive asyncResponse(...) method
                    generateRRUniResponse(targetMethod, targetClass, handledExceptionType, cc, rrAsyncResponseDescriptor,
                            returnType, handlesMultipleExceptions, commonHierarchyOfExceptions,
                            (method, contextHandle) -> method.readInstanceField(delegateField, method.getThis()),
                            unwrappableTypes);
                } else {
                    throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
                }
            }
            result.put(handledExceptionType.name().toString(), generatedClassName);
        }
        return result;
    }

    private static Type[] getHandledExceptionTypes(MethodInfo targetMethod) {
        AnnotationValue annotationValue = targetMethod.annotation(SERVER_EXCEPTION_MAPPER).value();
        // handle the case where 'value' is set
        if (annotationValue != null) {
            Type[] valueArray = annotationValue.asClassArray();
            if ((valueArray != null) && (valueArray.length > 0)) {
                return valueArray;
            }
        }

        // handle the case where we deduce the type of exception handler by the Throwable defined in method parameters
        Type deducedHandledExceptionType = null;
        List<Type> methodParameters = targetMethod.parameterTypes();
        for (Type methodParameter : methodParameters) {
            if (methodParameter.kind() == Type.Kind.CLASS) {
                Class<?> methodParameterClass = getClassByName(methodParameter.name().toString());
                if (methodParameterClass != null && Throwable.class.isAssignableFrom(methodParameterClass)) {
                    if (deducedHandledExceptionType != null) {
                        throw new IllegalArgumentException(
                                "Multiple method parameters found that extend 'Throwable'. When using '@ServerExceptionMapper', only one parameter can be of type 'Throwable'. Offending method is '"
                                        + targetMethod.name() + "' of class '"
                                        + targetMethod.declaringClass().name().toString() + "'");
                    }
                    deducedHandledExceptionType = methodParameter;
                }
            }
        }
        if (deducedHandledExceptionType == null) {
            throw new IllegalArgumentException(
                    "When '@ServerExceptionMapper' is used without a value, then the annotated method must contain a method parameter that extends 'Throwable'. Offending method is '"
                            + targetMethod.name() + "' of class '" + targetMethod.declaringClass().name().toString() + "'");
        }
        return new Type[] { deducedHandledExceptionType };
    }

    private static Set<String> getCommonHierarchyOfExceptions(Type[] handledExceptions) {
        Set<String> commonHierarchy = new HashSet<>();
        boolean first = true;
        for (Type handledException : handledExceptions) {
            Class<?> handledExceptionClass = getClassByName(handledException.name().toString());
            while (handledExceptionClass != null && !handledExceptionClass.equals(Throwable.class)) {
                String handledExceptionClassName = handledExceptionClass.getName();
                if (first) {
                    commonHierarchy.add(handledExceptionClassName);
                } else if (!commonHierarchy.contains(handledExceptionClassName)) {
                    commonHierarchy.remove(handledExceptionClassName);
                }

                handledExceptionClass = handledExceptionClass.getSuperclass();
            }

            first = false;
        }

        return commonHierarchy;
    }

    private static Class<?> getClassByName(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static MethodDescriptor toResponseDescriptor(Type handledExceptionType, String generatedClassName) {
        return ofMethod(generatedClassName, "toResponse",
                Response.class.getName(),
                handledExceptionType.name().toString(), ServerRequestContext.class.getName());
    }

    private static MethodDescriptor asyncResponseDescriptor(Type handledExceptionType, String generatedClassName) {
        return ofMethod(generatedClassName, "asyncResponse",
                void.class.getName(),
                handledExceptionType.name().toString(), AsyncExceptionMapperContext.class.getName());
    }

    private static Class<?> determineInterfaceType(ReturnType returnType) {
        if (returnType == ReturnType.RESPONSE || returnType == ReturnType.REST_RESPONSE) {
            return ResteasyReactiveExceptionMapper.class;
        } else if (returnType == ReturnType.UNI_RESPONSE || returnType == ReturnType.UNI_REST_RESPONSE) {
            return ResteasyReactiveAsyncExceptionMapper.class;
        }
        throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
    }

    private static void generateSpecToResponseBridge(Type handledExceptionType, ClassCreator cc,
            MethodDescriptor specToResponseDescriptor) {
        MethodCreator mc = cc.getMethodCreator("toResponse", Response.class, Throwable.class);
        ResultHandle bridgeSpecParam = mc.getMethodParam(0);
        ResultHandle castedBridgeSpecMethodParam = mc.checkCast(bridgeSpecParam,
                handledExceptionType.name().toString());
        mc.returnValue(mc.invokeVirtualMethod(specToResponseDescriptor,
                mc.getThis(), castedBridgeSpecMethodParam));
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
        ResultHandle castedBridgeRRMethodParam = bridgeRRToResponse.checkCast(bridgeRRExceptionParam,
                handledExceptionType.name().toString());
        bridgeRRToResponse.returnValue(bridgeRRToResponse.invokeVirtualMethod(rrToResponseDescriptor,
                bridgeRRToResponse.getThis(), castedBridgeRRMethodParam, bridgeRRContextParam));
    }

    private static void generateRRResponse(MethodInfo targetMethod, ClassInfo targetClass, Type handledExceptionType,
            ClassCreator cc, MethodDescriptor rrToResponseDescriptor,
            ReturnType returnType, boolean handlesMultipleExceptions, Set<String> commonHierarchyOfExceptions,
            BiFunction<MethodCreator, ResultHandle, ResultHandle> targetInstanceHandleCreator, Set<DotName> unwrappableTypes) {
        MethodCreator mc = cc.getMethodCreator(rrToResponseDescriptor);
        ResultHandle exceptionHandle = mc.getMethodParam(0);
        ResultHandle contextHandle = mc.checkCast(mc.getMethodParam(1),
                ResteasyReactiveRequestContext.class);
        ResultHandle targetInstanceHandle = targetInstanceHandleCreator.apply(mc, contextHandle);
        if (targetMethod.parameterTypes().isEmpty()) {
            // just call the target method with no parameters
            ResultHandle resultHandle = mc.invokeVirtualMethod(
                    ofMethod(targetClass.name().toString(), targetMethod.name(), targetMethod.returnType().name().toString()),
                    targetInstanceHandle);
            if (returnType == ReturnType.REST_RESPONSE) {
                resultHandle = mc.invokeVirtualMethod(ofMethod(RestResponse.class, "toResponse", Response.class),
                        resultHandle);
            }
            mc.returnValue(resultHandle);
        } else {
            TargetMethodParamsInfo targetMethodParamsInfo = getTargetMethodParamsInfo(targetMethod, targetClass,
                    handledExceptionType, mc, exceptionHandle, contextHandle, handlesMultipleExceptions,
                    commonHierarchyOfExceptions, unwrappableTypes);
            ResultHandle resultHandle = mc.invokeVirtualMethod(
                    ofMethod(targetClass.name().toString(), targetMethod.name(),
                            targetMethod.returnType().name().toString(), targetMethodParamsInfo.getTypes()),
                    targetInstanceHandle, targetMethodParamsInfo.getHandles());
            if (returnType == ReturnType.REST_RESPONSE) {
                resultHandle = mc.invokeVirtualMethod(ofMethod(RestResponse.class, "toResponse", Response.class),
                        resultHandle);
            }
            mc.returnValue(resultHandle);
        }
    }

    private static void generateRRUniResponseBridge(Type handledExceptionType, ClassCreator cc,
            MethodDescriptor rrAsyncResponseDescriptor) {
        MethodCreator mc = cc.getMethodCreator("asyncResponse", void.class, Throwable.class,
                AsyncExceptionMapperContext.class);
        ResultHandle bridgeRRExceptionParam = mc.getMethodParam(0);
        ResultHandle bridgeRRContextParam = mc.getMethodParam(1);
        ResultHandle castedBridgeRRMethodParam = mc.checkCast(bridgeRRExceptionParam,
                handledExceptionType.name().toString());
        mc.returnValue(mc.invokeVirtualMethod(rrAsyncResponseDescriptor,
                mc.getThis(), castedBridgeRRMethodParam, bridgeRRContextParam));
    }

    private static void generateRRUniResponse(MethodInfo targetMethod, ClassInfo targetClass, Type handledExceptionType,
            ClassCreator cc, MethodDescriptor rrAsyncResponseDescriptor,
            ReturnType returnType, boolean handlesMultipleExceptions, Set<String> commonHierarchyOfExceptions,
            BiFunction<MethodCreator, ResultHandle, ResultHandle> targetInstanceHandleCreator, Set<DotName> unwrappableTypes) {
        MethodCreator mc = cc.getMethodCreator(rrAsyncResponseDescriptor);
        ResultHandle exceptionHandle = mc.getMethodParam(0);
        ResultHandle asyncContextHandle = mc.getMethodParam(1);
        ResultHandle serverContextHandle = mc.invokeInterfaceMethod(
                ofMethod(AsyncExceptionMapperContext.class, "serverRequestContext", ServerRequestContext.class),
                asyncContextHandle);
        ResultHandle contextHandle = mc.checkCast(serverContextHandle, ResteasyReactiveRequestContext.class);
        ResultHandle targetInstanceHandle = targetInstanceHandleCreator.apply(mc, contextHandle);
        ResultHandle uniHandle;
        if (targetMethod.parameterTypes().isEmpty()) {
            // just call the target method with no parameters
            uniHandle = mc.invokeVirtualMethod(
                    ofMethod(targetClass.name().toString(), targetMethod.name(), Uni.class),
                    targetInstanceHandle);
        } else {
            TargetMethodParamsInfo targetMethodParamsInfo = getTargetMethodParamsInfo(targetMethod, targetClass,
                    handledExceptionType, mc, exceptionHandle, contextHandle, handlesMultipleExceptions,
                    commonHierarchyOfExceptions, unwrappableTypes);
            uniHandle = mc.invokeVirtualMethod(
                    ofMethod(targetClass.name().toString(), targetMethod.name(),
                            Uni.class.getName(), targetMethodParamsInfo.getTypes()),
                    targetInstanceHandle, targetMethodParamsInfo.getHandles());
        }
        String methodName = returnType == ReturnType.UNI_RESPONSE ? "handleUniResponse" : "handleUniRestResponse";
        mc.invokeStaticMethod(ofMethod(AsyncExceptionMappingUtil.class, methodName, void.class, Uni.class,
                AsyncExceptionMapperContext.class), uniHandle, asyncContextHandle);
        mc.returnValue(null);
    }

    private static TargetMethodParamsInfo getTargetMethodParamsInfo(MethodInfo targetMethod, ClassInfo targetClass,
            Type handledExceptionType, MethodCreator mc, ResultHandle exceptionHandle, ResultHandle contextHandle,
            boolean handlesMultipleExceptions, Set<String> commonHierarchyOfExceptions, Set<DotName> unwrappableTypes) {
        List<Type> parameters = targetMethod.parameterTypes();
        ResultHandle[] targetMethodParamHandles = new ResultHandle[parameters.size()];
        String[] parameterTypes = new String[parameters.size()];
        // TODO: we probably want to refactor this and remove duplicate code that also exists in CustomFilterGenerator
        for (int i = 0; i < parameters.size(); i++) {
            Type parameter = parameters.get(i);
            DotName paramDotName = parameter.name();
            parameterTypes[i] = paramDotName.toString();
            String parameterName = targetMethod.parameterName(i);
            if (paramDotName.equals(THROWABLE)) {
                targetMethodParamHandles[i] = exceptionHandle;
            } else if (paramDotName.equals(handledExceptionType.name())) {
                if (handlesMultipleExceptions && !commonHierarchyOfExceptions.contains(paramDotName.toString())) {
                    throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                            + " of class '" + targetClass.name() + "' cannot be of type '" + handledExceptionType.name()
                            + "' because the method handles multiple exceptions. You can use '"
                            + Throwable.class.getName() + "' instead.");
                } else {
                    targetMethodParamHandles[i] = exceptionHandle;
                }
            } else if (commonHierarchyOfExceptions.contains(paramDotName.toString())) {
                targetMethodParamHandles[i] = exceptionHandle;
            } else if (paramDotName.equals(handledExceptionType.name())) {
                targetMethodParamHandles[i] = exceptionHandle;
            } else if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)
                    || QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = mc.invokeVirtualMethod(
                        ofMethod(ResteasyReactiveRequestContext.class.getName(), "getContainerRequestContext",
                                ContainerRequestContextImpl.class),
                        contextHandle);
            } else if (SERVER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = contextHandle;
            } else if (URI_INFO.equals(paramDotName)) {
                paramHandleFromReqContextMethod(mc, contextHandle, targetMethodParamHandles, i,
                        "getUriInfo",
                        URI_INFO);
            } else if (HTTP_HEADERS.equals(paramDotName)) {
                paramHandleFromReqContextMethod(mc, contextHandle, targetMethodParamHandles, i,
                        "getHttpHeaders",
                        HttpHeadersImpl.class);
            } else if (REQUEST.equals(paramDotName)) {
                paramHandleFromReqContextMethod(mc, contextHandle, targetMethodParamHandles, i,
                        "getRequest",
                        REQUEST);
            } else if (unwrappableTypes.contains(paramDotName)) {
                targetMethodParamHandles[i] = unwrapObject(mc, contextHandle, paramDotName);
            } else if (RESOURCE_INFO.equals(paramDotName)) {
                ResultHandle runtimeResourceHandle = runtimeResourceHandle(mc, contextHandle);
                AssignableResultHandle resourceInfo = mc.createVariable(ResourceInfo.class);
                BranchResult ifNullBranch = mc.ifNull(runtimeResourceHandle);
                ifNullBranch.trueBranch().assign(resourceInfo, ifNullBranch.trueBranch().readStaticField(FieldDescriptor
                        .of(SimpleResourceInfo.NullValues.class, "INSTANCE", SimpleResourceInfo.NullValues.class)));
                ifNullBranch.falseBranch().assign(resourceInfo, ifNullBranch.falseBranch().invokeVirtualMethod(
                        ofMethod(RuntimeResource.class, "getLazyMethod", ResteasyReactiveResourceInfo.class),
                        runtimeResourceHandle));
                targetMethodParamHandles[i] = resourceInfo;
            } else if (SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                ResultHandle runtimeResourceHandle = runtimeResourceHandle(mc, contextHandle);
                AssignableResultHandle resourceInfo = mc.createVariable(SimpleResourceInfo.class);
                BranchResult ifNullBranch = mc.ifNull(runtimeResourceHandle);
                ifNullBranch.trueBranch().assign(resourceInfo, ifNullBranch.trueBranch().readStaticField(FieldDescriptor
                        .of(SimpleResourceInfo.NullValues.class, "INSTANCE", SimpleResourceInfo.NullValues.class)));
                ifNullBranch.falseBranch().assign(resourceInfo, ifNullBranch.falseBranch().invokeVirtualMethod(
                        ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimpleResourceInfo.class),
                        runtimeResourceHandle));
                targetMethodParamHandles[i] = resourceInfo;
            } else {
                throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                        + " of class '" + targetClass.name()
                        + "' is not allowed");
            }
        }
        return new TargetMethodParamsInfo(targetMethodParamHandles, parameterTypes);
    }

    private static ReturnType determineReturnType(MethodInfo targetMethod) {
        if (targetMethod.returnType().name().equals(RESPONSE)) {
            return ReturnType.RESPONSE;
        } else if (targetMethod.returnType().name().equals(REST_RESPONSE)) {
            return ReturnType.REST_RESPONSE;
        } else if (targetMethod.returnType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = targetMethod.returnType().asParameterizedType();
            if (parameterizedType.name().equals(UNI) && (parameterizedType.arguments().size() == 1)) {
                if (parameterizedType.arguments().get(0).name().equals(RESPONSE)) {
                    return ReturnType.UNI_RESPONSE;
                }
                if (parameterizedType.arguments().get(0).name().equals(REST_RESPONSE)) {
                    return ReturnType.UNI_REST_RESPONSE;
                }
            }
        }
        throw new RuntimeException("Method '" + targetMethod.name() + " of class '" + targetMethod.declaringClass().name()
                + "' cannot be used as an exception mapper as it does not declare 'Response' or 'Uni<Response>' or as its return type");
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
        return targetMethod.declaringClass().name() + "$ExceptionMapper$"
                + HashUtil.sha1(targetMethod.name() + handledExceptionType.name().toString());
    }

    private enum ReturnType {
        RESPONSE,
        REST_RESPONSE,
        UNI_RESPONSE,
        UNI_REST_RESPONSE
    }

    private static class TargetMethodParamsInfo {
        private final ResultHandle[] handles;
        private final String[] types;

        public TargetMethodParamsInfo(ResultHandle[] handles, String[] types) {
            this.handles = handles;
            this.types = types;
        }

        public ResultHandle[] getHandles() {
            return handles;
        }

        public String[] getTypes() {
            return types;
        }
    }
}
