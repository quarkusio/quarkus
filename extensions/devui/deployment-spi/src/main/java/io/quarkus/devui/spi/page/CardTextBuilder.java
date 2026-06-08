package io.quarkus.devui.spi.page;

public class CardTextBuilder {

    private String title;
    private String icon;
    private String staticText;
    private String dynamicText;
    private String streamingText;
    private String[] streamingTextParams;

    CardTextBuilder() {
    }

    public CardTextBuilder title(String title) {
        this.title = title;
        return this;
    }

    public CardTextBuilder icon(String icon) {
        this.icon = icon;
        return this;
    }

    public CardTextBuilder staticText(String text) {
        this.staticText = text;
        return this;
    }

    public CardTextBuilder dynamicText(String jsonRpcMethodName) {
        this.dynamicText = jsonRpcMethodName;
        return this;
    }

    public CardTextBuilder streamingText(String jsonRpcMultiMethodName) {
        this.streamingText = jsonRpcMultiMethodName;
        return this;
    }

    public CardTextBuilder streamingTextParams(String... params) {
        this.streamingTextParams = params;
        return this;
    }

    public CardText build() {
        if (staticText == null && dynamicText == null && streamingText == null) {
            throw new RuntimeException("CardText requires either staticText(), dynamicText(), or streamingText()");
        }
        String joinedParams = null;
        if (streamingTextParams != null && streamingTextParams.length > 0) {
            joinedParams = String.join(",", streamingTextParams);
        }
        return new CardText(title, icon, staticText, dynamicText, streamingText, joinedParams);
    }
}
