package io.quarkus.panache.rest.common.deployment;

import io.quarkus.gizmo.ClassCreator;

public interface MethodImplementor {

    void implement(ClassCreator classCreator);
}
