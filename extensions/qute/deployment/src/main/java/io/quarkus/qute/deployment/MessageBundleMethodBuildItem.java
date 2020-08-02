package io.quarkus.qute.deployment;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a message bundle method that has a template that needs to be validated.
 * <p>
 * Note that templates that contain no expressions don't need to be validated.
 */
public final class MessageBundleMethodBuildItem extends MultiBuildItem {

    private final String bundleName;
    private final String key;
    private final String templateId;
    private final MethodInfo method;
    private final String template;

    public MessageBundleMethodBuildItem(String bundleName, String key, String templateId, MethodInfo method, String template) {
        this.bundleName = bundleName;
        this.key = key;
        this.templateId = templateId;
        this.method = method;
        this.template = template;
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

}
