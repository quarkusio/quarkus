package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class CheckedTemplateAdapterBuildItem extends MultiBuildItem {

    public final CheckedTemplateAdapter adapter;

    public CheckedTemplateAdapterBuildItem(CheckedTemplateAdapter adapter) {
        this.adapter = adapter;
    }

}
