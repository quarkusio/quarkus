package io.quarkus.undertow.deployment;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.metadata.web.spec.WebMetaData;

public final class WebMetadataBuildItem extends SimpleBuildItem {

    private final WebMetaData webMetaData;

    public WebMetadataBuildItem(WebMetaData webMetaData) {
        this.webMetaData = webMetaData;
    }

    public WebMetaData getWebMetaData() {
        return webMetaData;
    }
}
