package io.quarkus.jaxrs.client.reactive.deployment.beanparam;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

abstract class ValueExtractor {
    abstract ResultHandle extract(BytecodeCreator bytecodeCreator, ResultHandle containingObject);
}
