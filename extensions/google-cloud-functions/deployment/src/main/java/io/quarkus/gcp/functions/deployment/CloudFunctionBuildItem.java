package io.quarkus.gcp.functions.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gcp.functions.GoogleCloudFunctionInfo;

final class CloudFunctionBuildItem extends MultiBuildItem {
    private String className;
    private String beanName;
    private GoogleCloudFunctionInfo.FunctionType functionType;

    CloudFunctionBuildItem(String className, GoogleCloudFunctionInfo.FunctionType functionType) {
        this.className = className;
        this.functionType = functionType;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public GoogleCloudFunctionInfo.FunctionType getFunctionType() {
        return functionType;
    }

    public void setFunctionType(GoogleCloudFunctionInfo.FunctionType functionType) {
        this.functionType = functionType;
    }

    public GoogleCloudFunctionInfo build() {
        GoogleCloudFunctionInfo info = new GoogleCloudFunctionInfo();
        info.setBeanName(this.beanName);
        info.setClassName(this.className);
        info.setFunctionType(this.functionType);
        return info;
    }
}
