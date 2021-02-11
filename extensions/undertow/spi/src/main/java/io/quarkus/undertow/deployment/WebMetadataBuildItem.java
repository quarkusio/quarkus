package io.quarkus.undertow.deployment;

import org.jboss.metadata.web.spec.WebMetaData;

import io.quarkus.builder.item.SimpleBuildItem;

public final class WebMetadataBuildItem extends SimpleBuildItem {

    private final WebMetaData webMetaData;

    public WebMetadataBuildItem(WebMetaData webMetaData) {
        this.webMetaData = webMetaData;
    }

    public WebMetaData getWebMetaData() {
        return webMetaData;
    }
}
