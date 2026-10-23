package io.quarkus.gcp.functions;

/**
 * Information about a Google Cloud Function discovered during build time.
 *
 * @param beanName the CDI bean name (from {@code @Named}), or {@code null} if unnamed
 * @param className the fully qualified class name of the function
 * @param functionType the type of the cloud function
 */
public record GoogleCloudFunctionInfo(
        String beanName,
        String className,
        FunctionType functionType) {

    /**
     * Get the bean name.
     *
     * @return the bean name, or {@code null} if unnamed
     */
    public String getBeanName() {
        return beanName;
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
     * Get the function type.
     *
     * @return the function type
     */
    public FunctionType getFunctionType() {
        return functionType;
    }

    /**
     * The type of Google Cloud Function.
     */
    public enum FunctionType {
        /** HTTP function. */
        HTTP,
        /** Background function. */
        BACKGROUND,
        /** Raw background function. */
        RAW_BACKGROUND,
        /** CloudEvents function. */
        CLOUD_EVENT
    }
}
