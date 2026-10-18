package org.jboss.resteasy.reactive.client.processor.beanparam;

import org.jboss.jandex.Type;

public class HeaderParamItem extends Item {
    private final String headerName;
    private final Type paramType;

    public HeaderParamItem(String fieldName, String headerName, ValueExtractor extractor, Type paramType) {
        super(fieldName, ItemType.HEADER_PARAM, false, extractor);
        this.headerName = headerName;
        this.paramType = paramType;
    }

    public String getHeaderName() {
        return headerName;
    }

    public Type getParamType() {
        return paramType;
    }
}
