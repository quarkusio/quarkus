package io.quarkus.spring.web.deployment;

import java.lang.annotation.Annotation;

import javax.ws.rs.core.Response;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.spring.web.runtime.ResponseEntityConverter;

class ControllerAdviceAbstractExceptionMapperGenerator extends AbstractExceptionMapperGenerator {

    private static final DotName RESPONSE_ENTITY = DotName.createSimple("org.springframework.http.ResponseEntity");

    private final MethodInfo controllerAdviceMethod;
    private final IndexView index;
    private final Type returnType;
    private final Type parameterType;
    private final String declaringClassName;

    ControllerAdviceAbstractExceptionMapperGenerator(MethodInfo controllerAdviceMethod, DotName exceptionDotName,
            ClassOutput classOutput, IndexView index) {
        super(exceptionDotName, classOutput);
        this.controllerAdviceMethod = controllerAdviceMethod;
        this.index = index;

        this.returnType = controllerAdviceMethod.returnType();
        this.parameterType = controllerAdviceMethod.parameters().size() == 0 ? null
                : controllerAdviceMethod.parameters().get(0);
        this.declaringClassName = controllerAdviceMethod.declaringClass().name().toString();
    }

    @Override
    void generateMethodBody(MethodCreator toResponse) {
        if (returnType.kind() == Type.Kind.VOID) {
            AnnotationInstance responseStatusInstance = controllerAdviceMethod.annotation(RESPONSE_STATUS);
            if (responseStatusInstance == null) {
                throw new IllegalStateException(
                        "void methods annotated with @ExceptionHandler must also be annotated with @ResponseStatus");
            }

            // invoke the @ExceptionHandler method
            exceptionHandlerMethodResponse(toResponse);

            // build a JAX-RS response
            ResultHandle status = toResponse.load(getHttpStatusFromAnnotation(responseStatusInstance));
            ResultHandle responseBuilder = toResponse.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Response.class, "status", Response.ResponseBuilder.class, int.class),
                    status);

            ResultHandle httpResponseType = toResponse.load("text/plain");
            toResponse.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "type", Response.ResponseBuilder.class,
                            String.class),
                    responseBuilder, httpResponseType);

            ResultHandle response = toResponse.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "build", Response.class),
                    responseBuilder);
            toResponse.returnValue(response);
        } else {
            ResultHandle exceptionHandlerMethodResponse = exceptionHandlerMethodResponse(toResponse);

            ResultHandle response;
            if (RESPONSE_ENTITY.equals(returnType.name())) {
                // convert Spring's ResponseEntity to JAX-RS Response
                response = toResponse.invokeStaticMethod(
                        MethodDescriptor.ofMethod(ResponseEntityConverter.class.getName(), "toResponse",
                                Response.class.getName(), RESPONSE_ENTITY.toString()),
                        exceptionHandlerMethodResponse);
            } else {
                ResultHandle status = toResponse.load(getStatus(controllerAdviceMethod.annotation(RESPONSE_STATUS)));

                ResultHandle responseBuilder = toResponse.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Response.class, "status", Response.ResponseBuilder.class, int.class),
                        status);

                toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "entity", Response.ResponseBuilder.class,
                                Object.class),
                        responseBuilder, exceptionHandlerMethodResponse);

                response = toResponse.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "build", Response.class),
                        responseBuilder);
            }

            toResponse.returnValue(response);
        }
    }

    private ResultHandle exceptionHandlerMethodResponse(MethodCreator toResponse) {
        String returnTypeClassName = returnType.kind() == Type.Kind.VOID ? void.class.getName() : returnType.name().toString();

        if (parameterType == null) {
            return toResponse.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(declaringClassName, controllerAdviceMethod.name(), returnTypeClassName),
                    controllerAdviceInstance(toResponse));
        }
        return toResponse.invokeVirtualMethod(
                MethodDescriptor.ofMethod(declaringClassName, controllerAdviceMethod.name(), returnTypeClassName,
                        parameterType.name().toString()),
                controllerAdviceInstance(toResponse), toResponse.getMethodParam(0));
    }

    private ResultHandle controllerAdviceInstance(MethodCreator toResponse) {
        ResultHandle controllerAdviceClass = toResponse.invokeStaticMethod(
                MethodDescriptor.ofMethod(Class.class, "forName", Class.class, String.class),
                toResponse.load(declaringClassName));

        ResultHandle container = toResponse
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instance = toResponse.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                container, controllerAdviceClass, toResponse.loadNull());
        ResultHandle bean = toResponse.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                instance);
        return toResponse.checkCast(bean, controllerAdviceMethod.declaringClass().name().toString());
    }

    private int getStatus(AnnotationInstance instance) {
        if (instance == null) {
            return 200;
        }
        return getHttpStatusFromAnnotation(instance);
    }
}
