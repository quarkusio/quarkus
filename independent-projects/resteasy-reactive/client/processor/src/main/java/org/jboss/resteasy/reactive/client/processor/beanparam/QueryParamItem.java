package org.jboss.resteasy.reactive.client.processor.beanparam;

import org.jboss.jandex.Type;

public class QueryParamItem extends Item {

    private final String name;
    private final Type valueType;

    public QueryParamItem(String name, ValueExtractor extractor, Type valueType) {
        super(ItemType.QUERY_PARAM, extractor);
        this.name = name;
        this.valueType = valueType;
    }

    public String name() {
        return name;
    }

    public Type getValueType() {
        return valueType;
    }
}
