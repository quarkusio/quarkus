package io.quarkus.rest.data.panache.runtime.hal;

public class HalLink {

    private final String href;

    public HalLink(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }
}
