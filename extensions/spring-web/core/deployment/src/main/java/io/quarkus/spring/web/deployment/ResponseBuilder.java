package io.quarkus.spring.web.deployment;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;

final class ResponseBuilder {

    private static final MethodDesc RESPONSE_STATUS = MethodDesc.of(Response.class, "status",
            Response.ResponseBuilder.class, int.class);
    private static final MethodDesc RESPONSE_BUILDER_BUILD = MethodDesc.of(Response.ResponseBuilder.class, "build",
            Response.class);
    private static final MethodDesc RESPONSE_BUILDER_TYPE = MethodDesc.of(Response.ResponseBuilder.class, "type",
            Response.ResponseBuilder.class, MediaType.class);
    private static final MethodDesc RESPONSE_BUILDER_ENTITY = MethodDesc.of(Response.ResponseBuilder.class, "entity",
            Response.ResponseBuilder.class, Object.class);

    private final BlockCreator bc;

    private final LocalVar delegate;

    ResponseBuilder(BlockCreator bc, int status) {
        this.bc = bc;
        this.delegate = bc.localVar("responseBuilder", bc.invokeStatic(RESPONSE_STATUS, Const.of(status)));
    }

    public Expr build() {
        return bc.invokeVirtual(RESPONSE_BUILDER_BUILD, delegate);
    }

    public ResponseBuilder withType(Expr type) {
        bc.invokeVirtual(RESPONSE_BUILDER_TYPE, delegate, type);
        return this;
    }

    public ResponseBuilder withEntity(Expr entity) {
        bc.invokeVirtual(RESPONSE_BUILDER_ENTITY, delegate, entity);
        return this;
    }
}
