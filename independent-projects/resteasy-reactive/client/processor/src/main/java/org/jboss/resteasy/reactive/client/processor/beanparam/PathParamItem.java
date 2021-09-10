package org.jboss.resteasy.reactive.client.processor.beanparam;

public class PathParamItem extends Item {

    private final String pathParamName;

    public PathParamItem(String pathParamName, ValueExtractor valueExtractor) {
        super(ItemType.PATH_PARAM, valueExtractor);
        this.pathParamName = pathParamName;
    }

    public String getPathParamName() {
        return pathParamName;
    }
}
