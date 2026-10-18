package io.quarkus.devui.spi.page;

public class CardText {

    private final String title;
    private final String icon;
    private final String staticText;
    private final String dynamicText;
    private final String streamingText;
    private final String streamingTextParams;

    CardText(String title, String icon, String staticText,
            String dynamicText, String streamingText, String streamingTextParams) {
        this.title = title;
        this.icon = icon;
        this.staticText = staticText;
        this.dynamicText = dynamicText;
        this.streamingText = streamingText;
        this.streamingTextParams = streamingTextParams;
    }

    public static CardTextBuilder textBuilder() {
        return new CardTextBuilder();
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public String getStaticText() {
        return staticText;
    }

    public String getDynamicText() {
        return dynamicText;
    }

    public String getStreamingText() {
        return streamingText;
    }

    public String getStreamingTextParams() {
        return streamingTextParams;
    }
}
