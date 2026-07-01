package io.quarkus.spring.security.deployment.roles;

import java.util.function.Function;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;

public interface HasRoleValueProducer extends Function<BlockCreator, Expr> {
}
