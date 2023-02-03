package io.quarkus.rest.client.reactive.deployment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.client.reactive.runtime.ResteasyReactiveResponseExceptionMapper;
import io.quarkus.runtime.util.HashUtil;

/**
 * Generates an implementation of {@link org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper}
 * from an instance of {@link io.quarkus.rest.client.reactive.ClientExceptionMapper}
 */
class ClientExceptionMapperHandler {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final ResultHandle[] EMPTY_RESULT_HANDLES_ARRAY = new ResultHandle[0];
    private static final MethodDescriptor GET_INVOKED_METHOD =
            MethodDescriptor.ofMethod(RestClientRequestContext.class, "getInvokedMethod", Method.class);
    private final ClassOutput classOutput;

    ClientExceptionMapperHandler(ClassOutput classOutput) {
        this.classOutput = classOutput;
    }

    /**
     * Generates an implementation of {@link ResponseExceptionMapper} that looks something like:
     *
     * <pre>
     * {@code
     *  public class SomeService_map_ResponseExceptionMapper_a8fb70beeef2a54b80151484d109618eed381626 implements ResteasyReactiveResponseExceptionMapper {
     *      public Throwable toThrowable(Response var1, RestClientRequestContext var2) {
     *          // simply call the static method of interface
     *          return SomeService.map(var1);
     *      }
     *
     * }
     * </pre>
     */
    Result generateResponseExceptionMapper(AnnotationInstance instance) {
        if (!DotNames.CLIENT_EXCEPTION_MAPPER.equals(instance.name())) {
            throw new IllegalArgumentException(
                    "'clientExceptionMapperInstance' must be an instance of " + DotNames.CLIENT_EXCEPTION_MAPPER);
        }
        MethodInfo targetMethod = null;
        boolean isValid = false;
        if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
            targetMethod = instance.target().asMethod();
            if (ignoreAnnotation(targetMethod)) {
                return null;
            }
            if ((targetMethod.flags() & Modifier.STATIC) != 0) {
                String returnTypeClassName = targetMethod.returnType().name().toString();
                try {
                    boolean returnsRuntimeException = RuntimeException.class.isAssignableFrom(
                            Class.forName(returnTypeClassName, false, Thread.currentThread().getContextClassLoader()));
                    if (returnsRuntimeException) {
                        isValid = true;
                    }
                } catch (ClassNotFoundException ignored) {

                }
            }
        }
        if (!isValid) {
            String message = DotNames.CLIENT_EXCEPTION_MAPPER
                    + " is only supported on static methods of REST Client interfaces that take 'jakarta.ws.rs.core.Response' as a single parameter and return 'java.lang.RuntimeException'.";
            if (targetMethod != null) {
                message += " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                        + targetMethod.name() + "'";
            }
            throw new IllegalStateException(message);
        }

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(targetMethod.name()).append("_").append(targetMethod.returnType().name().toString());
        for (Type i : targetMethod.parameterTypes()) {
            sigBuilder.append(i.name().toString());
        }

        int priority = Priorities.USER;
        AnnotationValue priorityAnnotationValue = instance.value("priority");
        if (priorityAnnotationValue != null) {
            priority = priorityAnnotationValue.asInt();
        }

        ClassInfo restClientInterfaceClassInfo = targetMethod.declaringClass();
        String generatedClassName = restClientInterfaceClassInfo.name().toString() + "_" + targetMethod.name() + "_"
                + "ResponseExceptionMapper" + "_" + HashUtil.sha1(sigBuilder.toString());
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput).className(generatedClassName)
                .interfaces(ResteasyReactiveResponseExceptionMapper.class).build()) {
            MethodCreator toThrowable = cc.getMethodCreator("toThrowable", Throwable.class, Response.class,
                    RestClientRequestContext.class);
            LinkedHashMap<String, ResultHandle> targetMethodParams = new LinkedHashMap<>();
            for (Type paramType : targetMethod.parameterTypes()) {
                ResultHandle targetMethodParamHandle;
                if (paramType.name().equals(ResteasyReactiveDotNames.RESPONSE)) {
                    targetMethodParamHandle = toThrowable.getMethodParam(0);
                } else if (paramType.name().equals(DotNames.METHOD)) {
                    targetMethodParamHandle = toThrowable.invokeVirtualMethod(GET_INVOKED_METHOD, toThrowable.getMethodParam(1));
                } else {
                    String message = DotNames.CLIENT_EXCEPTION_MAPPER + " can only take parameters of type '" + ResteasyReactiveDotNames.RESPONSE + "' or '" + DotNames.METHOD + "'"
                    + " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                            + targetMethod.name() + "'";
                    throw new IllegalStateException(message);
                }
                targetMethodParams.put(paramType.name().toString(), targetMethodParamHandle);
            }

            ResultHandle resultHandle = toThrowable.invokeStaticInterfaceMethod(
                    MethodDescriptor.ofMethod(
                            restClientInterfaceClassInfo.name().toString(),
                            targetMethod.name(),
                            targetMethod.returnType().name().toString(),
                            targetMethodParams.keySet().toArray(EMPTY_STRING_ARRAY)),
                    targetMethodParams.values().toArray(EMPTY_RESULT_HANDLES_ARRAY));
            toThrowable.returnValue(resultHandle);

            if (priority != Priorities.USER) {
                MethodCreator getPriority = cc.getMethodCreator("getPriority", int.class);
                getPriority.returnValue(getPriority.load(priority));
            }
        }

        return new Result(restClientInterfaceClassInfo.name().toString(), generatedClassName, priority);
    }

    private static boolean ignoreAnnotation(MethodInfo methodInfo) {
        // ignore the annotation if it's placed on a Kotlin companion class
        // this is not a problem since the Kotlin compiler will also place the annotation the static method interface method
        return methodInfo.declaringClass().name().toString().contains("$Companion");
    }

    static class Result {
        final String interfaceName;
        final String generatedClassName;
        final int priority;

        Result(String interfaceName, String generatedClassName, int priority) {
            this.interfaceName = interfaceName;
            this.generatedClassName = generatedClassName;
            this.priority = priority;
        }
    }
}
