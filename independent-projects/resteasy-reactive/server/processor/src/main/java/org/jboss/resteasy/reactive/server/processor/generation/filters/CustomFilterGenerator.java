package org.jboss.resteasy.reactive.server.processor.generation.filters;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTAINER_REQUEST_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONTAINER_RESPONSE_CONTEXT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HTTP_HEADERS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONAL;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REQUEST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESOURCE_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_RESPONSE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.THROWABLE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.URI_INFO;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.VOID;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.filters.FilterUtil;
import org.jboss.resteasy.reactive.server.filters.PreventAbortResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.HttpHeadersImpl;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.processor.generation.multipart.GeneratorUtils;
import org.jboss.resteasy.reactive.server.processor.util.KotlinUtils;
import org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.smallrye.mutiny.Uni;

/**
 * Generates the actual implementation of a provider that allows user code using annotations like
 * {@link ServerRequestFilter} and {@link ServerResponseFilter} to work seamlessly
 */
final class CustomFilterGenerator {

    private static final String ABSTRACT_SUSPENDED_REQ_FILTER = "org.jboss.resteasy.reactive.server.runtime.kotlin.AbstractSuspendedRequestFilter";
    private static final String ABSTRACT_SUSPENDED_RES_FILTER = "org.jboss.resteasy.reactive.server.runtime.kotlin.AbstractSuspendedResponseFilter";

    private final Set<DotName> unwrappableTypes;
    private final Set<String> additionalBeanAnnotations;
    private final Predicate<MethodInfo> isOptionalFilter;

    CustomFilterGenerator(Set<DotName> unwrappableTypes, Set<String> additionalBeanAnnotations,
            Predicate<MethodInfo> isOptionalFilter) {
        this.unwrappableTypes = unwrappableTypes;
        this.additionalBeanAnnotations = additionalBeanAnnotations;
        this.isOptionalFilter = isOptionalFilter;
    }

    String generateContainerRequestFilter(MethodInfo targetMethod, ClassOutput classOutput) {
        checkModifiers(targetMethod, ResteasyReactiveServerDotNames.SERVER_REQUEST_FILTER);
        if (KotlinUtils.isSuspendMethod(targetMethod)) {
            return generateRequestFilterForSuspendedMethod(targetMethod, classOutput, isOptionalFilter.test(targetMethod));
        }
        return generateStandardRequestFilter(targetMethod, classOutput, isOptionalFilter.test(targetMethod));
    }

    private String generateRequestFilterForSuspendedMethod(MethodInfo targetMethod, ClassOutput classOutput,
            boolean checkForOptionalBean) {
        DotName returnDotName = determineReturnDotNameOfSuspendMethod(targetMethod);
        ReturnType returnType;
        if (returnDotName.equals(VOID)) {
            returnType = ReturnType.VOID;
        } else if (returnDotName.equals(RESPONSE)) {
            returnType = ReturnType.RESPONSE;
        } else if (returnDotName.equals(REST_RESPONSE)) {
            returnType = ReturnType.REST_RESPONSE;
        } else {
            throw new RuntimeException("Suspend method '" + targetMethod.name() + " of class '"
                    + targetMethod.declaringClass().name()
                    + "' cannot be used as a request filter as it does not declare 'void', 'Response', 'RestResponse' as its return type.");
        }

        String generatedClassName = getGeneratedClassName(targetMethod, ResteasyReactiveServerDotNames.SERVER_REQUEST_FILTER);
        ClassInfo declaringClass = targetMethod.declaringClass();
        DotName declaringClassName = declaringClass.name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .superClass(ABSTRACT_SUSPENDED_REQ_FILTER)
                .build()) {
            FieldDescriptor delegateField = generateConstructorAndDelegateField(cc, declaringClass,
                    ABSTRACT_SUSPENDED_REQ_FILTER, additionalBeanAnnotations, checkForOptionalBean);

            // generate the implementation of the 'doFilter' method
            MethodCreator doFilterMethod = cc.getMethodCreator("doFilter", Object.class.getName(),
                    ResteasyReactiveContainerRequestContext.class.getName(), ResteasyReactiveDotNames.CONTINUATION.toString());
            ResultHandle delegate = doFilterMethod.readInstanceField(delegateField, doFilterMethod.getThis());

            if (checkForOptionalBean) {
                // if the delegate is null (because there was no bean of the target type, just return),
                doFilterMethod.ifNull(delegate).trueBranch().returnNull();
            }

            // call the target method
            ResultHandle resultHandle = doFilterMethod.invokeVirtualMethod(targetMethod,
                    delegate,
                    getRequestFilterResultHandles(targetMethod, declaringClassName, doFilterMethod, 2,
                            getRRReqCtxHandle(doFilterMethod, getRRContainerReqCtxHandle(doFilterMethod, 0))));
            doFilterMethod.returnValue(resultHandle);

            // generate the implementation of the 'handleResult' method which simply delegates the Uni handling
            // (which is created by AbstractFilterCoroutineInvoker) to FilterUtil
            MethodCreator handleResultMethod = cc.getMethodCreator("handleResult", void.class,
                    ResteasyReactiveContainerRequestContext.class, Uni.class);
            String methodName;
            switch (returnType) {
                case VOID:
                    methodName = "handleUniVoid";
                    break;
                case RESPONSE:
                    methodName = "handleUniResponse";
                    break;
                case REST_RESPONSE:
                    methodName = "handleUniRestResponse";
                    break;
                default:
                    throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported, in method "
                            + targetMethod.declaringClass() + "." + targetMethod.name());
            }
            handleResultMethod
                    .invokeStaticMethod(
                            MethodDescriptor.ofMethod(FilterUtil.class,
                                    methodName,
                                    void.class,
                                    Uni.class, ResteasyReactiveContainerRequestContext.class),
                            handleResultMethod.getMethodParam(1), getRRContainerReqCtxHandle(handleResultMethod, 0));

            handleResultMethod.returnNull();
        }

        return generatedClassName;
    }

    /**
     * Generates an implementation of {@link jakarta.ws.rs.container.ContainerRequestFilter} that delegates to the method
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
    private String generateStandardRequestFilter(MethodInfo targetMethod, ClassOutput classOutput,
            boolean checkForOptionalBean) {
        ReturnType returnType = determineRequestFilterReturnType(targetMethod);
        String generatedClassName = getGeneratedClassName(targetMethod, ResteasyReactiveServerDotNames.SERVER_REQUEST_FILTER);
        ClassInfo declaringClass = targetMethod.declaringClass();
        DotName declaringClassName = declaringClass.name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(determineRequestInterfaceType(returnType))
                .build()) {
            FieldDescriptor delegateField = generateConstructorAndDelegateField(cc, declaringClass,
                    Object.class.getName(), additionalBeanAnnotations, checkForOptionalBean);

            if (returnType == ReturnType.VOID
                    || returnType == ReturnType.OPTIONAL_RESPONSE
                    || returnType == ReturnType.OPTIONAL_REST_RESPONSE
                    || returnType == ReturnType.RESPONSE
                    || returnType == ReturnType.REST_RESPONSE) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class, ContainerRequestContext.class);
                ResultHandle delegate = filterMethod.readInstanceField(delegateField, filterMethod.getThis());

                if (checkForOptionalBean) {
                    // if the delegate is null (because there was no bean of the target type, just return),
                    BytecodeCreator delegateIsNull = filterMethod.ifNull(delegate).trueBranch();
                    if (returnType == ReturnType.VOID) {
                        delegateIsNull.returnVoid();
                    } else {
                        delegateIsNull.returnNull();
                    }
                }

                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                // call the target method

                ResultHandle resultHandle = filterMethod.invokeVirtualMethod(targetMethod,
                        delegate,
                        getRequestFilterResultHandles(targetMethod, declaringClassName, filterMethod, 1,
                                getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle)));
                if (returnType == ReturnType.OPTIONAL_RESPONSE) {
                    // invoke utility class that deals with Optional
                    filterMethod.invokeStaticMethod(MethodDescriptor.ofMethod(FilterUtil.class, "handleOptional", void.class,
                            Optional.class, ResteasyReactiveContainerRequestContext.class), resultHandle,
                            rrContainerReqCtxHandle);
                } else if (returnType == ReturnType.OPTIONAL_REST_RESPONSE) {
                    // invoke utility class that deals with Optional
                    filterMethod.invokeStaticMethod(
                            MethodDescriptor.ofMethod(FilterUtil.class, "handleOptionalRestResponse", void.class,
                                    Optional.class, ResteasyReactiveContainerRequestContext.class),
                            resultHandle,
                            rrContainerReqCtxHandle);
                } else if (returnType == ReturnType.RESPONSE) {
                    filterMethod.invokeStaticMethod(MethodDescriptor.ofMethod(FilterUtil.class, "handleResponse", void.class,
                            Response.class, ResteasyReactiveContainerRequestContext.class), resultHandle,
                            rrContainerReqCtxHandle);
                } else if (returnType == ReturnType.REST_RESPONSE) {
                    filterMethod.invokeStaticMethod(
                            MethodDescriptor.ofMethod(FilterUtil.class, "handleRestResponse", void.class,
                                    RestResponse.class, ResteasyReactiveContainerRequestContext.class),
                            resultHandle,
                            rrContainerReqCtxHandle);
                }
                filterMethod.returnValue(null);
            } else if (returnType == ReturnType.UNI_VOID
                    || returnType == ReturnType.UNI_RESPONSE
                    || returnType == ReturnType.UNI_REST_RESPONSE) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class,
                        ResteasyReactiveContainerRequestContext.class);

                if (checkForOptionalBean) {
                    // if the delegate is null (because there was no bean of the target type, just return),
                    filterMethod.ifNull(filterMethod.readInstanceField(delegateField, filterMethod.getThis())).trueBranch()
                            .returnNull();
                }

                // call the target method
                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                ResultHandle uniHandle = filterMethod.invokeVirtualMethod(targetMethod,
                        filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                        getRequestFilterResultHandles(targetMethod, declaringClassName, filterMethod, 1,
                                getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle)));
                String methodName;
                switch (returnType) {
                    case UNI_VOID:
                        methodName = "handleUniVoid";
                        break;
                    case UNI_RESPONSE:
                        methodName = "handleUniResponse";
                        break;
                    case UNI_REST_RESPONSE:
                        methodName = "handleUniRestResponse";
                        break;
                    default:
                        throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported, in method "
                                + targetMethod.declaringClass() + "." + targetMethod.name());
                }
                // invoke utility class that deals with suspend / resume
                filterMethod
                        .invokeStaticMethod(
                                MethodDescriptor.ofMethod(FilterUtil.class,
                                        methodName,
                                        void.class,
                                        Uni.class, ResteasyReactiveContainerRequestContext.class),
                                uniHandle, rrContainerReqCtxHandle);
                filterMethod.returnNull();
            } else {
                throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported, in method "
                        + targetMethod.declaringClass() + "." + targetMethod.name());
            }
        }
        return generatedClassName;
    }

    private ResultHandle[] getRequestFilterResultHandles(MethodInfo targetMethod, DotName declaringClassName,
            MethodCreator filterMethod, int filterMethodParamCount, ResultHandle rrReqCtxHandle) {
        // for each of the parameters of the user method, generate bytecode that pulls the argument outs of QuarkusRestRequestContext
        ResultHandle[] targetMethodParamHandles = new ResultHandle[targetMethod.parametersCount()];
        for (int i = 0; i < targetMethod.parametersCount(); i++) {
            Type param = targetMethod.parameterType(i);
            DotName paramDotName = param.name();
            if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.newInstance(
                        MethodDescriptor.ofConstructor(PreventAbortResteasyReactiveContainerRequestContext.class,
                                ContainerRequestContext.class),
                        filterMethod.getMethodParam(0));
                ;
            } else if (ResteasyReactiveServerDotNames.QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
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
            } else if (RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (ResteasyReactiveServerDotNames.SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getSimpleResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (ResteasyReactiveDotNames.CONTINUATION.equals(paramDotName)) {
                // the continuation to pass on to the target is retrieved from the last parameter of the filter method
                targetMethodParamHandles[i] = filterMethod.getMethodParam(filterMethodParamCount - 1);
            } else if (unwrappableTypes.contains(paramDotName)) {
                targetMethodParamHandles[i] = GeneratorUtils.unwrapObject(filterMethod, rrReqCtxHandle, paramDotName);
            } else {
                String parameterName = targetMethod.parameterName(i);
                throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                        + " of class '" + declaringClassName
                        + "' is not allowed");
            }
        }
        return targetMethodParamHandles;
    }

    String generateContainerResponseFilter(MethodInfo targetMethod, ClassOutput classOutput) {
        checkModifiers(targetMethod, ResteasyReactiveServerDotNames.SERVER_RESPONSE_FILTER);
        if (KotlinUtils.isSuspendMethod(targetMethod)) {
            return generateResponseFilterForSuspendedMethod(targetMethod, classOutput, isOptionalFilter.test(targetMethod));
        }
        return generateStandardContainerResponseFilter(targetMethod, classOutput, isOptionalFilter.test(targetMethod));
    }

    private String generateResponseFilterForSuspendedMethod(MethodInfo targetMethod, ClassOutput classOutput,
            boolean checkForOptionalBean) {
        DotName returnDotName = determineReturnDotNameOfSuspendMethod(targetMethod);
        if (!returnDotName.equals(VOID)) {
            throw new RuntimeException("Suspend method '" + targetMethod.name() + " of class '"
                    + targetMethod.declaringClass().name()
                    + "' cannot be used as a request filter as it does not declare 'void' as its return type.");
        }
        String generatedClassName = getGeneratedClassName(targetMethod, ResteasyReactiveServerDotNames.SERVER_RESPONSE_FILTER);
        ClassInfo declaringClass = targetMethod.declaringClass();
        DotName declaringClassName = declaringClass.name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .superClass(ABSTRACT_SUSPENDED_RES_FILTER)
                .build()) {
            FieldDescriptor delegateField = generateConstructorAndDelegateField(cc, declaringClass,
                    ABSTRACT_SUSPENDED_RES_FILTER, additionalBeanAnnotations, checkForOptionalBean);

            // generate the implementation of the filter method
            MethodCreator doFilterMethod = cc.getMethodCreator("doFilter", Object.class.getName(),
                    ResteasyReactiveContainerRequestContext.class.getName(), ContainerResponseContext.class.getName(),
                    ResteasyReactiveDotNames.CONTINUATION.toString());

            ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(doFilterMethod, 0);
            ResultHandle rrReqCtxHandle = getRRReqCtxHandle(doFilterMethod, rrContainerReqCtxHandle);

            // call the target method
            ResultHandle resultHandle = doFilterMethod.invokeVirtualMethod(targetMethod,
                    doFilterMethod.readInstanceField(delegateField, doFilterMethod.getThis()),
                    getResponseFilterResultHandles(targetMethod, declaringClassName,
                            doFilterMethod, 3, rrReqCtxHandle));
            doFilterMethod.returnValue(resultHandle);
        }
        return generatedClassName;
    }

    /**
     * Generates an implementation of {@link jakarta.ws.rs.container.ContainerResponseFilter} that delegates to the method
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
    private String generateStandardContainerResponseFilter(MethodInfo targetMethod, ClassOutput classOutput,
            boolean checkForOptionalBean) {
        ReturnType returnType = determineResponseFilterReturnType(targetMethod);
        String generatedClassName = getGeneratedClassName(targetMethod, ResteasyReactiveServerDotNames.SERVER_RESPONSE_FILTER);
        ClassInfo declaringClassInfo = targetMethod.declaringClass();
        DotName declaringClassName = declaringClassInfo.name();
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(determineResponseInterfaceType(returnType))
                .build()) {
            FieldDescriptor delegateField = generateConstructorAndDelegateField(cc, declaringClassInfo,
                    Object.class.getName(), additionalBeanAnnotations, checkForOptionalBean);

            if (returnType == ReturnType.VOID) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class, ContainerRequestContext.class,
                        ContainerResponseContext.class);

                if (checkForOptionalBean) {
                    filterMethod.ifNull(filterMethod.readInstanceField(delegateField, filterMethod.getThis())).trueBranch()
                            .returnVoid();
                }

                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                ResultHandle rrReqCtxHandle = getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle);

                // call the target method
                filterMethod.invokeVirtualMethod(targetMethod,
                        filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                        getResponseFilterResultHandles(targetMethod, declaringClassName,
                                filterMethod, 2, rrReqCtxHandle));
                filterMethod.returnValue(null);
            } else if (returnType == ReturnType.UNI_VOID) {
                // generate the implementation of the filter method
                MethodCreator filterMethod = cc.getMethodCreator("filter", void.class,
                        ResteasyReactiveContainerRequestContext.class,
                        ContainerResponseContext.class);

                if (checkForOptionalBean) {
                    filterMethod.ifNull(filterMethod.readInstanceField(delegateField, filterMethod.getThis())).trueBranch()
                            .returnNull();
                }

                ResultHandle rrContainerReqCtxHandle = getRRContainerReqCtxHandle(filterMethod, 0);
                ResultHandle rrReqCtxHandle = getRRReqCtxHandle(filterMethod, rrContainerReqCtxHandle);

                // call the target method
                ResultHandle uniHandle = filterMethod.invokeVirtualMethod(targetMethod,
                        filterMethod.readInstanceField(delegateField, filterMethod.getThis()),
                        getResponseFilterResultHandles(targetMethod, declaringClassName,
                                filterMethod, 2, rrReqCtxHandle));

                // invoke utility class that deals with Optional
                filterMethod.invokeStaticMethod(
                        MethodDescriptor.ofMethod(FilterUtil.class, "handleUniVoid", void.class,
                                Uni.class, ResteasyReactiveContainerRequestContext.class),
                        uniHandle, rrContainerReqCtxHandle);
                filterMethod.returnValue(null);
            }
        }
        return generatedClassName;
    }

    private FieldDescriptor generateConstructorAndDelegateField(ClassCreator cc, ClassInfo declaringClass,
            String superClassName, Set<String> additionalBeanAnnotations,
            boolean checkForOptionalBean) {

        String declaringClassName = declaringClass.toString();
        ScopeInspectionResult scopeInspectionResult = inspectScope(declaringClass);

        if (scopeInspectionResult.scopeToAdd != null) {
            cc.addAnnotation(scopeInspectionResult.scopeToAdd);
        }
        for (String i : additionalBeanAnnotations) {
            cc.addAnnotation(i);
        }

        FieldDescriptor delegateField = cc.getFieldCreator("delegate", declaringClassName)
                .setModifiers(Modifier.PRIVATE | Modifier.FINAL)
                .getFieldDescriptor();

        MethodCreator ctor;
        if (checkForOptionalBean) {
            // generate a constructor that takes the Instance<TargetClass> as an argument
            ctor = cc.getMethodCreator("<init>", void.class, Instance.class).setSignature(
                    String.format("(L%s<L%s;>;)V",
                            Instance.class.getName().replace('.', '/'),
                            declaringClassName.replace('.', '/')));
        } else {
            // generate a constructor that takes the target class as an argument - this class is a CDI bean so Arc will be able to inject into the generated class
            ctor = cc.getMethodCreator("<init>", void.class, declaringClassName);
        }
        ctor.setModifiers(Modifier.PUBLIC);
        ctor.addAnnotation(Inject.class);
        ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(superClassName), ctor.getThis());
        ResultHandle self = ctor.getThis();
        if (checkForOptionalBean) {
            ResultHandle instance = ctor.getMethodParam(0);
            ResultHandle isResolvable = ctor
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(Instance.class, "isResolvable", boolean.class), instance);
            BranchResult isResolvableBranch = ctor.ifTrue(isResolvable);

            BytecodeCreator isResolvableTrue = isResolvableBranch.trueBranch();
            isResolvableTrue.writeInstanceField(delegateField, self, isResolvableTrue
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(Instance.class, "get", Object.class), instance));

            BytecodeCreator isResolvableFalse = isResolvableBranch.falseBranch();
            isResolvableFalse.writeInstanceField(delegateField, self, isResolvableFalse.loadNull());
        } else {
            ctor.writeInstanceField(delegateField, self, ctor.getMethodParam(0));
        }

        ctor.returnValue(null);

        if (scopeInspectionResult.needsProxy) {
            // generate no-args constructor needed for creating proxies
            MethodCreator noArgsCtor = cc.getMethodCreator("<init>", void.class);
            noArgsCtor.setModifiers(Modifier.PUBLIC);
            noArgsCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(superClassName), noArgsCtor.getThis());
            noArgsCtor.writeInstanceField(delegateField, noArgsCtor.getThis(), noArgsCtor.loadNull());
            noArgsCtor.returnValue(null);
        }

        return delegateField;
    }

    /**
     * The generated class needs to be proxyable:
     *
     * <ul>
     * <li>The class declaring the filter annotation is itself annotated with {@code @ApplicationScoped}</li>
     * <li>The class contains instance fields that are not annotated with {@code Inject}. The reason for this rule
     * is to ensure that fields will not be instantiated at static-init time, which can cause problems during
     * native image build, see <a href="https://github.com/quarkusio/quarkus/issues/27752">this</a>
     * for more details</li>
     * </ul>
     *
     * If the class already has a scope declared, we use it. Otherwise, if it needs a proxy we make it
     * {@code @ApplicationScoped},
     * or {@code @Singleton} if no proxy is needed.
     */
    private ScopeInspectionResult inspectScope(ClassInfo classInfo) {
        if (classInfo.hasDeclaredAnnotation(ApplicationScoped.class) || classInfo.hasDeclaredAnnotation(RequestScoped.class)) {
            return new ScopeInspectionResult(ApplicationScoped.class, true);
        }
        if (classInfo.hasDeclaredAnnotation(Singleton.class)) {
            return new ScopeInspectionResult(Singleton.class, false);
        }
        List<FieldInfo> fields = classInfo.fields();
        if (fields.isEmpty()) {
            return new ScopeInspectionResult(Singleton.class, false);
        } else {
            boolean hasFieldWithoutInject = false;
            for (FieldInfo field : fields) {
                if (!field.hasAnnotation(Inject.class)) {
                    hasFieldWithoutInject = true;
                    break;
                }
            }
            return new ScopeInspectionResult(hasFieldWithoutInject ? ApplicationScoped.class : Singleton.class,
                    hasFieldWithoutInject);
        }
    }

    private static class ScopeInspectionResult {
        private final Class<?> scopeToAdd;
        private final boolean needsProxy;

        public ScopeInspectionResult(Class<?> scopeToAdd, boolean needsProxy) {
            this.scopeToAdd = scopeToAdd;
            this.needsProxy = needsProxy;
        }
    }

    private ResultHandle[] getResponseFilterResultHandles(MethodInfo targetMethod, DotName declaringClassName,
            MethodCreator filterMethod, int filterMethodParamCount, ResultHandle rrReqCtxHandle) {
        ResultHandle[] targetMethodParamHandles = new ResultHandle[targetMethod.parametersCount()];
        for (int i = 0; i < targetMethod.parametersCount(); i++) {
            Type param = targetMethod.parameterType(i);
            DotName paramDotName = param.name();
            if (CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.newInstance(
                        MethodDescriptor.ofConstructor(PreventAbortResteasyReactiveContainerRequestContext.class,
                                ContainerRequestContext.class),
                        filterMethod.getMethodParam(0));
            } else if (ResteasyReactiveServerDotNames.QUARKUS_REST_CONTAINER_REQUEST_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.checkCast(filterMethod.getMethodParam(0),
                        ResteasyReactiveContainerRequestContext.class);
            } else if (CONTAINER_RESPONSE_CONTEXT.equals(paramDotName)) {
                targetMethodParamHandles[i] = filterMethod.getMethodParam(1);
            } else if (unwrappableTypes.contains(paramDotName)) {
                targetMethodParamHandles[i] = GeneratorUtils.unwrapObject(filterMethod, rrReqCtxHandle, paramDotName);
            } else if (URI_INFO.equals(paramDotName)) {
                GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, rrReqCtxHandle, targetMethodParamHandles,
                        i,
                        "getUriInfo",
                        URI_INFO);
            } else if (RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (ResteasyReactiveServerDotNames.SIMPLIFIED_RESOURCE_INFO.equals(paramDotName)) {
                targetMethodParamHandles[i] = getSimpleResourceInfoHandle(filterMethod, rrReqCtxHandle);
            } else if (THROWABLE.equals(paramDotName)) {
                GeneratorUtils.paramHandleFromReqContextMethod(filterMethod, rrReqCtxHandle, targetMethodParamHandles, i,
                        "getThrowable",
                        THROWABLE);
            } else if (ResteasyReactiveDotNames.CONTINUATION.equals(paramDotName)) {
                // the continuation to pass on to the target is retrieved from the last parameter of the filter method
                targetMethodParamHandles[i] = filterMethod.getMethodParam(filterMethodParamCount - 1);
            } else {
                String parameterName = targetMethod.parameterName(i);
                throw new RuntimeException("Parameter '" + parameterName + "' of method '" + targetMethod.name()
                        + " of class '" + declaringClassName
                        + "' is not allowed");
            }
        }
        return targetMethodParamHandles;
    }

    private ResultHandle getRRContainerReqCtxHandle(MethodCreator filter, int containerReqCtxParamIndex) {
        ResultHandle containerReqCtxHandle = filter.getMethodParam(containerReqCtxParamIndex);
        return filter.checkCast(containerReqCtxHandle,
                ResteasyReactiveContainerRequestContext.class);
    }

    private ResultHandle getRRReqCtxHandle(MethodCreator filter, ResultHandle rrContainerReqCtxHandle) {
        ResultHandle rrCtxHandle = filter.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ResteasyReactiveContainerRequestContext.class, "getServerRequestContext",
                        ServerRequestContext.class),
                rrContainerReqCtxHandle);
        return filter.checkCast(rrCtxHandle, ResteasyReactiveRequestContext.class);
    }

    private AssignableResultHandle getResourceInfoHandle(MethodCreator filterMethod, ResultHandle rrReqCtxHandle) {
        ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, rrReqCtxHandle);
        AssignableResultHandle resourceInfo = filterMethod.createVariable(ResourceInfo.class);
        BranchResult ifNullBranch = filterMethod.ifNull(runtimeResourceHandle);
        ifNullBranch.trueBranch().assign(resourceInfo, ifNullBranch.trueBranch().readStaticField(
                FieldDescriptor.of(SimpleResourceInfo.NullValues.class, "INSTANCE", SimpleResourceInfo.NullValues.class)));
        ifNullBranch.falseBranch().assign(resourceInfo, ifNullBranch.falseBranch().invokeVirtualMethod(
                MethodDescriptor.ofMethod(RuntimeResource.class, "getLazyMethod", ResteasyReactiveResourceInfo.class),
                runtimeResourceHandle));
        return resourceInfo;
    }

    private AssignableResultHandle getSimpleResourceInfoHandle(MethodCreator filterMethod, ResultHandle rrReqCtxHandle) {
        ResultHandle runtimeResourceHandle = GeneratorUtils.runtimeResourceHandle(filterMethod, rrReqCtxHandle);
        AssignableResultHandle resourceInfo = filterMethod.createVariable(SimpleResourceInfo.class);
        BranchResult ifNullBranch = filterMethod.ifNull(runtimeResourceHandle);
        ifNullBranch.trueBranch().assign(resourceInfo, ifNullBranch.trueBranch().readStaticField(
                FieldDescriptor.of(SimpleResourceInfo.NullValues.class, "INSTANCE", SimpleResourceInfo.NullValues.class)));
        ifNullBranch.falseBranch().assign(resourceInfo, ifNullBranch.falseBranch().invokeVirtualMethod(
                MethodDescriptor.ofMethod(RuntimeResource.class, "getSimplifiedResourceInfo", SimpleResourceInfo.class),
                runtimeResourceHandle));
        return resourceInfo;
    }

    private String getGeneratedClassName(MethodInfo targetMethod, DotName annotationDotName) {
        DotName declaringClassName = targetMethod.declaringClass().name();
        return declaringClassName.toString() + "$Generated" + annotationDotName.withoutPackagePrefix() + "$"
                + targetMethod.name();
    }

    private void checkModifiers(MethodInfo info, DotName annotationDotName) {
        if ((info.flags() & Modifier.PRIVATE) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be private as it is annotated with '@" + annotationDotName + "'");
        }
        if ((info.flags() & Modifier.STATIC) != 0) {
            throw new RuntimeException("Method '" + info.name() + " of class '" + info.declaringClass().name()
                    + "' cannot be static as it is annotated with '@" + annotationDotName + "'");
        }
    }

    private ReturnType determineRequestFilterReturnType(MethodInfo targetMethod) {
        if (targetMethod.returnType().kind() == Type.Kind.VOID) {
            return ReturnType.VOID;
        } else if (targetMethod.returnType().kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = targetMethod.returnType().asParameterizedType();
            if (parameterizedType.name().equals(UNI) && (parameterizedType.arguments().size() == 1)) {
                if (parameterizedType.arguments().get(0).name().equals(VOID)) {
                    return ReturnType.UNI_VOID;
                } else if (parameterizedType.arguments().get(0).name().equals(RESPONSE)) {
                    return ReturnType.UNI_RESPONSE;
                } else if (parameterizedType.arguments().get(0).name().equals(REST_RESPONSE)) {
                    return ReturnType.UNI_REST_RESPONSE;
                }
            } else if (parameterizedType.name().equals(OPTIONAL) && (parameterizedType.arguments().size() == 1)) {
                if (parameterizedType.arguments().get(0).name().equals(RESPONSE)) {
                    return ReturnType.OPTIONAL_RESPONSE;
                } else if (parameterizedType.arguments().get(0).name().equals(REST_RESPONSE)) {
                    return ReturnType.OPTIONAL_REST_RESPONSE;
                }
            } else if (parameterizedType.name().equals(REST_RESPONSE)) {
                return ReturnType.REST_RESPONSE;
            }
        } else if (targetMethod.returnType().name().equals(RESPONSE)) {
            return ReturnType.RESPONSE;
        }
        throw new RuntimeException("Method '" + targetMethod.name() + " of class '" + targetMethod.declaringClass().name()
                + "' cannot be used as a request filter as it does not declare 'void', Response, RestResponse, Optional<Response>, Optional<RestResponse>, 'Uni<Void>', 'Uni<RestResponse>' or 'Uni<Response>' as its return type");
    }

    private ReturnType determineResponseFilterReturnType(MethodInfo targetMethod) {
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

    private Class<?> determineRequestInterfaceType(ReturnType returnType) {
        switch (returnType) {
            case VOID:
            case OPTIONAL_RESPONSE:
            case OPTIONAL_REST_RESPONSE:
            case RESPONSE:
            case REST_RESPONSE:
                return ContainerRequestFilter.class;
            case UNI_RESPONSE:
            case UNI_REST_RESPONSE:
            case UNI_VOID:
                return ResteasyReactiveContainerRequestFilter.class;
            default:
                throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
        }
    }

    private Class<?> determineResponseInterfaceType(ReturnType returnType) {
        if (returnType == ReturnType.VOID) {
            return ContainerResponseFilter.class;
        } else if (returnType == ReturnType.UNI_VOID) {
            return ResteasyReactiveContainerResponseFilter.class;
        }
        throw new IllegalStateException("ReturnType: '" + returnType + "' is not supported");
    }

    private DotName determineReturnDotNameOfSuspendMethod(MethodInfo methodInfo) {
        Type lastParamType = methodInfo.parameterType(methodInfo.parametersCount() - 1);
        if (lastParamType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            throw new IllegalStateException("Something went wrong during parameter type resolution - expected "
                    + lastParamType + " to be a Continuation with a generic type");
        }
        lastParamType = lastParamType.asParameterizedType().arguments().get(0);
        if (lastParamType.kind() != Type.Kind.WILDCARD_TYPE) {
            throw new IllegalStateException("Something went wrong during parameter type resolution - expected "
                    + lastParamType + " to be a Continuation with a generic type");
        }
        lastParamType = lastParamType.asWildcardType().superBound();
        if (lastParamType.name().equals(ResteasyReactiveDotNames.KOTLIN_UNIT)) {
            return ResteasyReactiveDotNames.VOID;
        }
        return lastParamType.name();
    }

    private enum ReturnType {
        VOID,
        RESPONSE,
        REST_RESPONSE,
        OPTIONAL_RESPONSE,
        OPTIONAL_REST_RESPONSE,
        UNI_VOID,
        UNI_RESPONSE,
        UNI_REST_RESPONSE
    }
}
