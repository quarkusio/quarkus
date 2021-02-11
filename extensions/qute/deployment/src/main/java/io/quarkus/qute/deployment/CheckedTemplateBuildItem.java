package io.quarkus.qute.deployment;

import java.util.Map;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class CheckedTemplateBuildItem extends MultiBuildItem {

    public final String templateId;
    public final Map<String, String> bindings;
    public final MethodInfo method;

    public CheckedTemplateBuildItem(String templateId, Map<String, String> bindings, MethodInfo method) {
        this.templateId = templateId;
        this.bindings = bindings;
        this.method = method;
    }

}
