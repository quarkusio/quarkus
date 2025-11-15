package io.quarkus.rest.client.reactive.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;

import jakarta.ws.rs.Priorities;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.rest.client.reactive.runtime.ResteasyReactiveContextResolver;
import io.quarkus.runtime.util.HashUtil;

/**
 * Generates an implementation of {@link ResteasyReactiveContextResolver}
 *
 * The extension will search for methods annotated with a special annotation like `@ClientObjectMapper` (if the REST Client
 * Jackson extension is present) and create the context resolver to register a custom object into the client context like the
 * ObjectMapper instance.
 */
class ClientContextResolverHandler {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final ResultHandle[] EMPTY_RESULT_HANDLES_ARRAY = new ResultHandle[0];
    private static final MethodDescriptor GET_INVOKED_METHOD = MethodDescriptor.ofMethod(RestClientRequestContext.class,
            "getInvokedMethod", Method.class);

    private final DotName annotation;
    private final Class<?> expectedReturnType;
    private final ClassOutput classOutput;

    ClientContextResolverHandler(DotName annotation, Class<?> expectedReturnType, ClassOutput classOutput) {
        this.annotation = annotation;
        this.expectedReturnType = expectedReturnType;
        this.classOutput = classOutput;
    }

    /**
     * Generates an implementation of {@link ResteasyReactiveContextResolver} that looks something like:
     *
     * <pre>
     * {@code
     *  public class SomeService_map_ContextResolver_a8fb70beeef2a54b80151484d109618eed381626
     *      implements ResteasyReactiveContextResolver<T> {
     *
     *      public T getContext(Class<?> type) {
     *          // simply call the static method of interface
     *          return SomeService.map(var1);
     *      }
     *
     * }
     * </pre>
     */
    GeneratedClassResult generateContextResolver(AnnotationInstance instance) {
        if (!annotation.equals(instance.name())) {
            throw new IllegalArgumentException(
                    "'clientContextResolverInstance' must be an instance of " + annotation);
        }
        MethodInfo targetMethod = findTargetMethod(instance);
        if (targetMethod == null) {
            return null;
        }

        int priority = Priorities.USER;
        AnnotationValue priorityAnnotationValue = instance.value("priority");
        if (priorityAnnotationValue != null) {
            priority = priorityAnnotationValue.asInt();
        }

        Class<?> returnTypeClassName = lookupReturnClass(targetMethod);
        if (!expectedReturnType.isAssignableFrom(returnTypeClassName)) {
            throw new IllegalStateException(annotation
                    + " is only supported on static methods of REST Client interfaces that return '" + expectedReturnType + "'."
                    + " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                    + targetMethod.name() + "'");
        }

        ClassInfo restClientInterfaceClassInfo = targetMethod.declaringClass();
        String generatedClassName = getGeneratedClassName(targetMethod);
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput).className(generatedClassName)
                .signature(SignatureBuilder.forClass()
                        .addInterface(io.quarkus.gizmo.Type.parameterizedType(
                                io.quarkus.gizmo.Type.classType(ResteasyReactiveContextResolver.class),
                                io.quarkus.gizmo.Type.classType(returnTypeClassName))))
                .build()) {
            MethodCreator getContext = cc.getMethodCreator("getContext", Object.class, Class.class);
            LinkedHashMap<String, ResultHandle> targetMethodParams = new LinkedHashMap<>();
            for (Type paramType : targetMethod.parameterTypes()) {
                ResultHandle targetMethodParamHandle;
                if (paramType.name().equals(DotNames.METHOD)) {
                    targetMethodParamHandle = getContext.invokeVirtualMethod(GET_INVOKED_METHOD, getContext.getMethodParam(1));
                } else {
                    targetMethodParamHandle = getFromCDI(getContext, targetMethod.returnType().name().toString());
                }

                targetMethodParams.put(paramType.name().toString(), targetMethodParamHandle);
            }

            ResultHandle resultHandle = getContext.invokeStaticInterfaceMethod(
                    MethodDescriptor.ofMethod(
                            restClientInterfaceClassInfo.name().toString(),
                            targetMethod.name(),
                            targetMethod.returnType().name().toString(),
                            targetMethodParams.keySet().toArray(EMPTY_STRING_ARRAY)),
                    targetMethodParams.values().toArray(EMPTY_RESULT_HANDLES_ARRAY));
            getContext.returnValue(resultHandle);
        }

        return new GeneratedClassResult(restClientInterfaceClassInfo.name().toString(), generatedClassName, priority);
    }

    private MethodInfo findTargetMethod(AnnotationInstance instance) {
        MethodInfo targetMethod = null;
        if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
            targetMethod = instance.target().asMethod();
            if (ignoreAnnotation(targetMethod)) {
                return null;
            }
            if ((targetMethod.flags() & Modifier.STATIC) != 0) {
                if (targetMethod.returnType().kind() == Type.Kind.VOID) {
                    throw new IllegalStateException(annotation
                            + " is only supported on static methods of REST Client interfaces that return an object."
                            + " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                            + targetMethod.name() + "'");
                }

            }
        }

        return targetMethod;
    }

    private static Class<?> lookupReturnClass(MethodInfo targetMethod) {
        Class<?> returnTypeClassName = null;
        try {
            returnTypeClassName = Class.forName(targetMethod.returnType().name().toString(), false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ignored) {

        }
        return returnTypeClassName;
    }

    private static ResultHandle getFromCDI(MethodCreator getContext, String className) {
        ResultHandle containerHandle = getContext
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = getContext.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                containerHandle, getContext.loadClassFromTCCL(className),
                getContext.newArray(Annotation.class, 0));
        return getContext.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                instanceHandle);
    }

    public static String getGeneratedClassName(MethodInfo methodInfo) {
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(methodInfo.name()).append("_").append(methodInfo.returnType().name().toString());
        for (Type i : methodInfo.parameterTypes()) {
            sigBuilder.append(i.name().toString());
        }

        return methodInfo.declaringClass().name().toString() + "_" + methodInfo.name() + "_"
                + "ContextResolver" + "_" + HashUtil.sha1(sigBuilder.toString());
    }

    private static boolean ignoreAnnotation(MethodInfo methodInfo) {
        // ignore the annotation if it's placed on a Kotlin companion class
        // this is not a problem since the Kotlin compiler will also place the annotation the static method interface method
        return methodInfo.declaringClass().name().toString().contains("$Companion");
    }
}
