package io.quarkus.hal;

public class HalLink {

    private final String href;

    public HalLink(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }
}
