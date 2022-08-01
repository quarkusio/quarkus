package org.jboss.resteasy.reactive.client.processor.beanparam;

public class CookieParamItem extends Item {
    private final String cookieName;
    private final String paramType;

    public CookieParamItem(String cookieName, ValueExtractor extractor, String paramType) {
        super(ItemType.COOKIE, extractor);
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
