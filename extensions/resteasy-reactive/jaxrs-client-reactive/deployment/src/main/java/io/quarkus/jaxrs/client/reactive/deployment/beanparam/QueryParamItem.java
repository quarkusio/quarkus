package io.quarkus.jaxrs.client.reactive.deployment.beanparam;

public class QueryParamItem extends Item {

    private final String name;

    public QueryParamItem(String name, ValueExtractor extractor) {
        super(ItemType.QUERY_PARAM, extractor);
        this.name = name;
    }

    public String name() {
        return name;
    }
}
