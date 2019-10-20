package io.quarkus.cxf.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class WebServiceBuildItem extends MultiBuildItem {
    private final String webServiceClass;

    public WebServiceBuildItem(String webServiceClass) {
        this.webServiceClass = webServiceClass;
    }

    public String getWebServiceClass() {
        return webServiceClass;
    }
}
