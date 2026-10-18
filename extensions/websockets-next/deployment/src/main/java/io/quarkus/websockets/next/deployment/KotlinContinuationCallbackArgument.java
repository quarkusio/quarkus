package io.quarkus.websockets.next.deployment;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import io.quarkus.arc.processor.KotlinDotNames;
import io.quarkus.arc.processor.KotlinUtils;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;

public class KotlinContinuationCallbackArgument implements CallbackArgument {
    @Override
    public boolean matches(ParameterContext context) {
        return KotlinUtils.isKotlinContinuationParameter(context.parameter());
    }

    @Override
    public Expr get(InvocationBytecodeContext context) {
        // the actual value is provided by the invoker
        return Const.ofNull(classDescOf(KotlinDotNames.CONTINUATION));
    }
}
