package io.quarkus.resteasy.reactive.client.deployment.beanparam;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

abstract class ValueExtractor {
    abstract ResultHandle extract(BytecodeCreator bytecodeCreator, ResultHandle containingObject);
}
