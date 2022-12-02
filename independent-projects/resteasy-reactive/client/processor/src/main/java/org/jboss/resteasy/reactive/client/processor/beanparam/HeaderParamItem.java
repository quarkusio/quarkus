package org.jboss.resteasy.reactive.client.processor.beanparam;

public class HeaderParamItem extends Item {
    private final String headerName;
    private final String paramType;

    public HeaderParamItem(String headerName, ValueExtractor extractor, String paramType) {
        super(ItemType.HEADER_PARAM, extractor);
        this.headerName = headerName;
        this.paramType = paramType;
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getParamType() {
        return paramType;
    }
}
