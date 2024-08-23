package io.quarkus.websockets.next.deployment;

import io.quarkus.arc.processor.KotlinUtils;
import io.quarkus.gizmo.ResultHandle;

public class KotlinContinuationCallbackArgument implements CallbackArgument {
    @Override
    public boolean matches(ParameterContext context) {
        return KotlinUtils.isKotlinContinuationParameter(context.parameter());
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        // the actual value is provided by the invoker
        return context.bytecode().loadNull();
    }
}
