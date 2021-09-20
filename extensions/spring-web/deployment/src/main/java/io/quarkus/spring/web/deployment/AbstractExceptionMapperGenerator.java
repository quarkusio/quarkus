package io.quarkus.spring.web.deployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;

abstract class AbstractExceptionMapperGenerator {

    protected static final DotName RESPONSE_STATUS = DotName
            .createSimple("org.springframework.web.bind.annotation.ResponseStatus");

    protected final DotName exceptionDotName;
    protected final ClassOutput classOutput;

    AbstractExceptionMapperGenerator(DotName exceptionDotName, ClassOutput classOutput) {
        this.exceptionDotName = exceptionDotName;
        this.classOutput = classOutput;
    }

    abstract void generateMethodBody(MethodCreator toResponse);

    String generate() {
        String generatedClassName = "io.quarkus.spring.web.mappers." + exceptionDotName.withoutPackagePrefix() + "_Mapper_"
                + HashUtil.sha1(exceptionDotName.toString());
        String exceptionClassName = exceptionDotName.toString();

        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(generatedClassName)
                .interfaces(ExceptionMapper.class)
                .signature(String.format("Ljava/lang/Object;Ljavax/ws/rs/ext/ExceptionMapper<L%s;>;",
                        exceptionClassName.replace('.', '/')))
                .build()) {

            preGenerateMethodBody(cc);

            try (MethodCreator toResponse = cc.getMethodCreator("toResponse", Response.class.getName(), exceptionClassName)) {
                generateMethodBody(toResponse);
            }

            // bridge method
            try (MethodCreator bridgeToResponse = cc.getMethodCreator("toResponse", Response.class, Throwable.class)) {
                MethodDescriptor toResponse = MethodDescriptor.ofMethod(generatedClassName, "toResponse",
                        Response.class.getName(), exceptionClassName);
                ResultHandle castedObject = bridgeToResponse.checkCast(bridgeToResponse.getMethodParam(0), exceptionClassName);
                ResultHandle result = bridgeToResponse.invokeVirtualMethod(toResponse, bridgeToResponse.getThis(),
                        castedObject);
                bridgeToResponse.returnValue(result);
            }
        }

        return generatedClassName;
    }

    protected void preGenerateMethodBody(ClassCreator cc) {

    }

    protected int getHttpStatusFromAnnotation(AnnotationInstance responseStatusInstance) {
        AnnotationValue code = responseStatusInstance.value("code");
        if (code != null) {
            return enumValueToHttpStatus(code.asString());
        }

        AnnotationValue value = responseStatusInstance.value();
        if (value != null) {
            return enumValueToHttpStatus(value.asString());
        }

        return 500; // the default value of @ResponseStatus
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private int enumValueToHttpStatus(String enumValue) {
        try {
            Class<?> httpStatusClass = Class.forName("org.springframework.http.HttpStatus");
            Enum correspondingEnum = Enum.valueOf((Class<Enum>) httpStatusClass, enumValue);
            Method valueMethod = httpStatusClass.getDeclaredMethod("value");
            return (int) valueMethod.invoke(correspondingEnum);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No spring web dependency found on the build classpath");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
