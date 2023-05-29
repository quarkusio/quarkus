package org.jboss.resteasy.reactive.client.processor.beanparam;

public class HeaderParamItem extends Item {
    private final String headerName;
    private final String paramType;

    public HeaderParamItem(String fieldName, String headerName, ValueExtractor extractor, String paramType) {
        super(fieldName, ItemType.HEADER_PARAM, false, extractor);
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
