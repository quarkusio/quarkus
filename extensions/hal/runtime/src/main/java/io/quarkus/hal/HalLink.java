package io.quarkus.hal;

public class HalLink {

    private final String href;
    private final String title;
    private final String type;

    public HalLink(String href, String title, String type) {
        this.href = href;
        this.title = title;
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }
}
