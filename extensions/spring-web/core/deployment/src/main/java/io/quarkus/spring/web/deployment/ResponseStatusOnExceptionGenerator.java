package io.quarkus.spring.web.deployment;

import javax.ws.rs.core.Response;

import org.jboss.jandex.ClassInfo;

import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

class ResponseStatusOnExceptionGenerator extends AbstractExceptionMapperGenerator {

    private final ClassInfo exceptionClassInfo;
    private final boolean isResteasyClassic;

    ResponseStatusOnExceptionGenerator(ClassInfo exceptionClassInfo, ClassOutput classOutput, boolean isResteasyClassic) {
        super(exceptionClassInfo.name(), classOutput);
        this.exceptionClassInfo = exceptionClassInfo;
        this.isResteasyClassic = isResteasyClassic;
    }

    void generateMethodBody(MethodCreator toResponse) {
        ResultHandle status = toResponse.load(getHttpStatusFromAnnotation(exceptionClassInfo.classAnnotation(RESPONSE_STATUS)));
        ResultHandle responseBuilder = toResponse.invokeStaticMethod(
                MethodDescriptor.ofMethod(Response.class, "status", Response.ResponseBuilder.class, int.class),
                status);
        ResultHandle exceptionMessage = toResponse.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Throwable.class, "getMessage", String.class),
                toResponse.getMethodParam(0));
        toResponse.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "entity", Response.ResponseBuilder.class,
                        Object.class),
                responseBuilder, exceptionMessage);
        ResultHandle httpResponseType = toResponse.load("text/plain");
        toResponse.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "type", Response.ResponseBuilder.class,
                        String.class),
                responseBuilder, httpResponseType);

        ResultHandle response = toResponse.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Response.ResponseBuilder.class, "build", Response.class),
                responseBuilder);
        toResponse.returnValue(response);
    }
}
