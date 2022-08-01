package org.jboss.resteasy.reactive.client.processor.beanparam;

public class PathParamItem extends Item {

    private final String pathParamName;
    private final String paramType;

    public PathParamItem(String pathParamName, String paramType, ValueExtractor valueExtractor) {
        super(ItemType.PATH_PARAM, valueExtractor);
        this.pathParamName = pathParamName;
        this.paramType = paramType;
    }

    public String getPathParamName() {
        return pathParamName;
    }

    public String getParamType() {
        return paramType;
    }
}
