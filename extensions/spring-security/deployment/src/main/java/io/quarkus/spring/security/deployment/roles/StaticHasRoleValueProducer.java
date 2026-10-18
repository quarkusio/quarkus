package io.quarkus.spring.security.deployment.roles;

import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;

public class StaticHasRoleValueProducer implements HasRoleValueProducer {

    private final String value;

    public StaticHasRoleValueProducer(String value) {
        this.value = value;
    }

    @Override
    public Expr apply(BlockCreator creator) {
        return Const.of(value);
    }
}
