package io.quarkus.qute.deployment;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;

/**
 * Represents a message bundle method.
 * <p>
 * Note that templates that contain no expressions/sections don't need to be validated.
 */
public final class MessageBundleMethodBuildItem extends MultiBuildItem {

    private final String bundleName;
    private final String key;
    private final String templateId;
    private final MethodInfo method;
    private final String template;
    private final boolean isDefaultBundle;
    private final boolean hasGeneratedTemplate;

    MessageBundleMethodBuildItem(String bundleName, String key, String templateId, MethodInfo method, String template,
            boolean isDefaultBundle, boolean hasGeneratedTemplate) {
        this.bundleName = bundleName;
        this.key = key;
        this.templateId = templateId;
        this.method = method;
        this.template = template;
        this.isDefaultBundle = isDefaultBundle;
        this.hasGeneratedTemplate = hasGeneratedTemplate;
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getKey() {
        return key;
    }

    /**
     *
     * @return the template id or {@code null} if there is no need to use qute; i.e. no expression/section found
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * For example, there is no corresponding method for generated enum constant message keys.
     *
     * @return the method or {@code null} if there is no corresponding method declared on the message bundle interface
     */
    public MethodInfo getMethod() {
        return method;
    }

    /**
     *
     * @return {@code true} if there is a corresponding method declared on the message bundle interface
     * @see #getMethod()
     */
    public boolean hasMethod() {
        return method != null;
    }

    public String getTemplate() {
        return template;
    }

    /**
     * A bundle method that does not need to be validated has {@code null} template id.
     *
     * @return {@code true} if the template needs to be validated
     */
    public boolean isValidatable() {
        return templateId != null;
    }

    /**
     *
     * @return {@code true} if the message comes from the default bundle
     */
    public boolean isDefaultBundle() {
        return isDefaultBundle;
    }

    /**
     *
     * @return {@code true} if the template was generated, e.g. a message bundle method for an enum
     */
    public boolean hasGeneratedTemplate() {
        return hasGeneratedTemplate;
    }

    /**
     *
     * @return the path
     * @see TemplateAnalysis#path
     */
    public String getPathForAnalysis() {
        if (method != null) {
            return method.declaringClass().name() + "#" + method.name();
        }
        if (templateId != null) {
            return templateId;
        }
        return bundleName + "_" + key;
    }

}
