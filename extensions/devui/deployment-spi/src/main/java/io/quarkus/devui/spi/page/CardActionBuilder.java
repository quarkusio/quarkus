package io.quarkus.devui.spi.page;

public class CardActionBuilder {

    private String title;
    private String icon;
    private String tooltip;
    private CardAction.ActionType actionType;
    private String actionReference;
    private boolean showResultNotification = true;

    CardActionBuilder() {
    }

    public CardActionBuilder title(String title) {
        this.title = title;
        return this;
    }

    public CardActionBuilder icon(String icon) {
        this.icon = icon;
        return this;
    }

    public CardActionBuilder tooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public CardActionBuilder jsonRpcMethodName(String methodName) {
        this.actionType = CardAction.ActionType.JSONRPC;
        this.actionReference = methodName;
        return this;
    }

    public CardActionBuilder url(String url) {
        this.actionType = CardAction.ActionType.URL;
        this.actionReference = url;
        return this;
    }

    public CardActionBuilder showResultNotification(boolean show) {
        this.showResultNotification = show;
        return this;
    }

    public CardAction build() {
        if (title == null || title.isEmpty()) {
            throw new RuntimeException("CardAction title is required");
        }
        if (actionType == null || actionReference == null || actionReference.isEmpty()) {
            throw new RuntimeException("CardAction requires either jsonRpcMethodName() or url()");
        }
        return new CardAction(title, icon, tooltip,
                actionType, actionReference, showResultNotification);
    }
}
