package io.quarkus.spring.security.deployment.roles;

import java.util.function.Function;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public interface HasRoleValueProducer extends Function<BytecodeCreator, ResultHandle> {
}
