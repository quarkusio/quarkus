package io.quarkus.azure.functions.deployment;

import java.lang.reflect.Method;

import io.quarkus.builder.item.MultiBuildItem;

public final class AzureFunctionBuildItem extends MultiBuildItem {
    private final String functionName;
    private final Class declaring;
    private final Method method;

    public AzureFunctionBuildItem(String functionName, Class declaring, Method method) {
        this.functionName = functionName;
        this.declaring = declaring;
        this.method = method;
    }

    public Class getDeclaring() {
        return declaring;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Method getMethod() {
        return method;
    }
}
