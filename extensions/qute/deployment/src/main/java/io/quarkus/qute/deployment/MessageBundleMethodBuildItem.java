package io.quarkus.qute.deployment;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a message bundle method.
 * <p>
 * Note that templates that contain no expressions don't need to be validated.
 */
public final class MessageBundleMethodBuildItem extends MultiBuildItem {

    private final String bundleName;
    private final String key;
    private final String templateId;
    private final MethodInfo method;
    private final String template;
    private final boolean isDefaultBundle;

    MessageBundleMethodBuildItem(String bundleName, String key, String templateId, MethodInfo method, String template,
            boolean isDefaultBundle) {
        this.bundleName = bundleName;
        this.key = key;
        this.templateId = templateId;
        this.method = method;
        this.template = template;
        this.isDefaultBundle = isDefaultBundle;
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getKey() {
        return key;
    }

    public String getTemplateId() {
        return templateId;
    }

    public MethodInfo getMethod() {
        return method;
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

}
