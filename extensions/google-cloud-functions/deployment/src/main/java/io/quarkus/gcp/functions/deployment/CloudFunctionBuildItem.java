package io.quarkus.gcp.functions.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gcp.functions.GoogleCloudFunctionInfo;

/**
 * Build item representing a discovered Google Cloud Function.
 */
final class CloudFunctionBuildItem extends MultiBuildItem {
    private String className;
    private String beanName;
    private GoogleCloudFunctionInfo.FunctionType functionType;

    /**
     * Construct a new cloud function build item.
     *
     * @param className the fully qualified class name of the function
     * @param functionType the type of the cloud function
     */
    CloudFunctionBuildItem(String className, GoogleCloudFunctionInfo.FunctionType functionType) {
        this.className = className;
        this.functionType = functionType;
    }

    /**
     * Get the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the class name.
     *
     * @param className the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the bean name.
     *
     * @return the bean name
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Set the bean name.
     *
     * @param beanName the bean name
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * Get the function type.
     *
     * @return the function type
     */
    public GoogleCloudFunctionInfo.FunctionType getFunctionType() {
        return functionType;
    }

    /**
     * Set the function type.
     *
     * @param functionType the function type
     */
    public void setFunctionType(GoogleCloudFunctionInfo.FunctionType functionType) {
        this.functionType = functionType;
    }

    /**
     * Build a {@link GoogleCloudFunctionInfo} from this build item.
     *
     * @return the google cloud function info
     */
    public GoogleCloudFunctionInfo build() {
        return new GoogleCloudFunctionInfo(this.beanName, this.className, this.functionType);
    }
}
