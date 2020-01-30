package io.quarkus.security.deployment;

import java.util.function.Function;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

/**
 * Used as an integration point when extensions need to customize the security behavior of a bean
 * The ResultHandle that is returned by function needs to be an instance of SecurityCheck
 */
public final class AdditionalSecurityCheckBuildItem extends MultiBuildItem {

    private final MethodInfo methodInfo;
    private final Function<BytecodeCreator, ResultHandle> function;

    public AdditionalSecurityCheckBuildItem(MethodInfo methodInfo, Function<BytecodeCreator, ResultHandle> function) {
        this.methodInfo = methodInfo;
        this.function = function;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public Function<BytecodeCreator, ResultHandle> getSecurityCheckResultHandleCreator() {
        return function;
    }
}
