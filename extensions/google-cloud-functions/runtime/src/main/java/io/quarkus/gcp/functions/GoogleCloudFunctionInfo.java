package io.quarkus.gcp.functions;

public class GoogleCloudFunctionInfo {
    private String beanName;
    private String className;
    private FunctionType functionType;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public void setFunctionType(FunctionType functionType) {
        this.functionType = functionType;
    }

    public static enum FunctionType {
        HTTP,
        BACKGROUND,
        RAW_BACKGROUND;
    }
}
