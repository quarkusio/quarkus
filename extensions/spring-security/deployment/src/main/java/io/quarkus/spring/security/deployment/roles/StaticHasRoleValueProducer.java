package io.quarkus.spring.security.deployment.roles;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public class StaticHasRoleValueProducer implements HasRoleValueProducer {

    private final String value;

    public StaticHasRoleValueProducer(String value) {
        this.value = value;
    }

    @Override
    public ResultHandle apply(BytecodeCreator creator) {
        return creator.load(value);
    }
}
