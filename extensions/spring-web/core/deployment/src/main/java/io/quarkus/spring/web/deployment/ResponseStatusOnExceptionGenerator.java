package io.quarkus.spring.web.deployment;

import jakarta.ws.rs.core.Response;

import org.jboss.jandex.ClassInfo;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;

class ResponseStatusOnExceptionGenerator extends AbstractExceptionMapperGenerator {

    private final ClassInfo exceptionClassInfo;

    ResponseStatusOnExceptionGenerator(ClassInfo exceptionClassInfo, ClassOutput classOutput,
            boolean isResteasyClassic) {
        super(exceptionClassInfo.name(), classOutput, isResteasyClassic);
        this.exceptionClassInfo = exceptionClassInfo;
    }

    void generateMethodBody(BlockCreator bc, Expr thisRef, Expr exceptionParam) {
        Expr status = Const.of(getHttpStatusFromAnnotation(exceptionClassInfo.declaredAnnotation(RESPONSE_STATUS)));
        LocalVar responseBuilder = bc.localVar("responseBuilder", bc.invokeStatic(
                MethodDesc.of(Response.class, "status", Response.ResponseBuilder.class, int.class),
                status));
        Expr exceptionMessage = bc.invokeVirtual(
                MethodDesc.of(Throwable.class, "getMessage", String.class),
                exceptionParam);
        bc.invokeVirtual(
                MethodDesc.of(Response.ResponseBuilder.class, "entity", Response.ResponseBuilder.class,
                        Object.class),
                responseBuilder, exceptionMessage);
        Expr httpResponseType = Const.of("text/plain");
        bc.invokeVirtual(
                MethodDesc.of(Response.ResponseBuilder.class, "type", Response.ResponseBuilder.class,
                        String.class),
                responseBuilder, httpResponseType);

        Expr response = bc.invokeVirtual(
                MethodDesc.of(Response.ResponseBuilder.class, "build", Response.class),
                responseBuilder);
        bc.return_(response);
    }
}
