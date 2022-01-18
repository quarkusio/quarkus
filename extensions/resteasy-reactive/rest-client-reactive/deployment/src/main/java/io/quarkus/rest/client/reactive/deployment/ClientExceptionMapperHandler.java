package io.quarkus.rest.client.reactive.deployment;

import java.lang.reflect.Modifier;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;

/**
 * Generates an implementation of {@link org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper}
 * from an instance of {@link io.quarkus.rest.client.reactive.ClientExceptionMapper}
 */
class ClientExceptionMapperHandler {

    private final ClassOutput classOutput;

    ClientExceptionMapperHandler(ClassOutput classOutput) {
        this.classOutput = classOutput;
    }

    Result generateResponseExceptionMapper(AnnotationInstance instance) {
        if (!DotNames.CLIENT_EXCEPTION_MAPPER.equals(instance.name())) {
            throw new IllegalArgumentException(
                    "'clientExceptionMapperInstance' must be an instance of " + DotNames.CLIENT_EXCEPTION_MAPPER);
        }
        MethodInfo targetMethod = null;
        boolean isValid = false;
        if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
            if ((instance.target().asMethod().flags() & Modifier.STATIC) != 0) {
                targetMethod = instance.target().asMethod();
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
                    + " is only supported on static methods of REST Client interfaces that take 'javax.ws.rs.core.Response' as a single parameter and return 'java.lang.RuntimeException'.";
            if (targetMethod != null) {
                message += " Offending instance is '" + targetMethod.declaringClass().name().toString() + "#"
                        + targetMethod.name() + "'";
            }
            throw new IllegalStateException(message);
        }

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(targetMethod.name()).append("_").append(targetMethod.returnType().name().toString());
        for (Type i : targetMethod.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        ClassInfo restClientInterfaceClassInfo = targetMethod.declaringClass();
        String generatedClassName = restClientInterfaceClassInfo.name().toString() + "_" + targetMethod.name() + "_"
                + "ResponseExceptionMapper" + "_" + HashUtil.sha1(sigBuilder.toString());
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput).className(generatedClassName)
                .interfaces(ResponseExceptionMapper.class).build()) {
            MethodCreator mc = cc.getMethodCreator("toThrowable", Throwable.class, Response.class);
            ResultHandle resultHandle = mc.invokeStaticInterfaceMethod(
                    MethodDescriptor.ofMethod(
                            restClientInterfaceClassInfo.name().toString(),
                            targetMethod.name(),
                            targetMethod.returnType().name().toString(),
                            targetMethod.parameters().get(0).name().toString()),
                    mc.getMethodParam(0));
            mc.returnValue(resultHandle);
        }

        int priority = Priorities.USER;
        if (instance.value() != null) {
            priority = instance.value().asInt();
        }
        return new Result(restClientInterfaceClassInfo.name().toString(), generatedClassName, priority);
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
