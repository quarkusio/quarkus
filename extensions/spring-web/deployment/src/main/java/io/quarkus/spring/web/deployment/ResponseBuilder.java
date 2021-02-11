package io.quarkus.spring.web.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

final class ResponseBuilder {

    private final MethodCreator methodCreator;

    private final ResultHandle delegate;

    ResponseBuilder(MethodCreator methodCreator, int status) {
        this.methodCreator = methodCreator;
        this.delegate = withStatus(status);
    }

    public ResultHandle build() {
        return methodCreator.invokeVirtualMethod(
                ofMethod(Response.ResponseBuilder.class, "build", Response.class), delegate);
    }

    public ResponseBuilder withType(ResultHandle type) {
        methodCreator.invokeVirtualMethod(
                ofMethod(Response.ResponseBuilder.class, "type", Response.ResponseBuilder.class, MediaType.class),
                delegate, type);
        return this;
    }

    public ResponseBuilder withEntity(ResultHandle entity) {
        methodCreator.invokeVirtualMethod(
                ofMethod(Response.ResponseBuilder.class, "entity", Response.ResponseBuilder.class, Object.class),
                delegate, entity);
        return this;
    }

    private ResultHandle withStatus(int status) {
        return methodCreator.invokeStaticMethod(
                ofMethod(Response.class, "status", Response.ResponseBuilder.class, int.class),
                methodCreator.load(status));
    }
}
