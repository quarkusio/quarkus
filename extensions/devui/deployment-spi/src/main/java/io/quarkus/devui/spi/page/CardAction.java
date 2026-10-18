package io.quarkus.devui.spi.page;

public class CardAction {

    public enum ActionType {
        JSONRPC,
        URL
    }

    private final String title;
    private final String icon;
    private final String tooltip;
    private final ActionType actionType;
    private final String actionReference;
    private final boolean showResultNotification;

    CardAction(String title, String icon, String tooltip,
            ActionType actionType, String actionReference, boolean showResultNotification) {
        this.title = title;
        this.icon = icon;
        this.tooltip = tooltip;
        this.actionType = actionType;
        this.actionReference = actionReference;
        this.showResultNotification = showResultNotification;
    }

    public static CardActionBuilder actionBuilder() {
        return new CardActionBuilder();
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public String getTooltip() {
        return tooltip;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getActionReference() {
        return actionReference;
    }

    public boolean isShowResultNotification() {
        return showResultNotification;
    }
}
