package io.quarkus.jaxrs.client.reactive.deployment.beanparam;

public class HeaderParamItem extends Item {
    private final String headerName;

    public HeaderParamItem(String headerName, ValueExtractor extractor) {
        super(ItemType.HEADER_PARAM, extractor);
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return headerName;
    }
}
