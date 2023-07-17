package org.jboss.resteasy.reactive.client.processor.beanparam;

public class CookieParamItem extends Item {
    private final String cookieName;
    private final String paramType;

    public CookieParamItem(String fieldName, String cookieName, ValueExtractor extractor, String paramType) {
        super(fieldName, ItemType.COOKIE, false, extractor);
        this.cookieName = cookieName;
        this.paramType = paramType;
    }

    public String getCookieName() {
        return cookieName;
    }

    public String getParamType() {
        return paramType;
    }
}
