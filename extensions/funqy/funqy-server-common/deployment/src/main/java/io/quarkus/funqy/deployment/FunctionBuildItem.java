package io.quarkus.funqy.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class FunctionBuildItem extends MultiBuildItem {
    protected String className;
    protected String methodName;
    protected String descriptor;
    protected String functionName;

    public FunctionBuildItem(String className, String methodName, String descriptor, String functionName) {
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.functionName = functionName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getFunctionName() {
        return functionName;
    }
}
