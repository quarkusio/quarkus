package io.quarkus.qute.deployment;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

public final class CheckedTemplateBuildItem extends MultiBuildItem {

    public final String templateId;
    public final Map<String, String> bindings;

    public CheckedTemplateBuildItem(String templateId, Map<String, String> bindings) {
        this.templateId = templateId;
        this.bindings = bindings;
    }

}
