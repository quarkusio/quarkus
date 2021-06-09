package io.quarkus.jaxrs.client.reactive.deployment.beanparam;

public class CookieParamItem extends Item {
    private final String cookieName;

    public CookieParamItem(String cookieName, ValueExtractor extractor) {
        super(ItemType.COOKIE, extractor);
        this.cookieName = cookieName;
    }

    public String getCookieName() {
        return cookieName;
    }
}
