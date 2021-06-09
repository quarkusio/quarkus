package io.quarkus.qute.deployment;

import java.util.Map;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.CheckedTemplate;

/**
 * Represents a method of a class annotated with {@link CheckedTemplate}.
 */
public final class CheckedTemplateBuildItem extends MultiBuildItem {

    // A template path, potentially incomplete
    public final String templateId;

    public final Map<String, String> bindings;
    public final MethodInfo method;
    public final boolean requireTypeSafeExpressions;

    public CheckedTemplateBuildItem(String templateId, Map<String, String> bindings, MethodInfo method,
            boolean requireTypeSafeExpressions) {
        this.templateId = templateId;
        this.bindings = bindings;
        this.method = method;
        this.requireTypeSafeExpressions = requireTypeSafeExpressions;
    }

}
