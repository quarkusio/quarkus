package io.quarkus.devtools.codestarts;

import io.quarkus.devtools.messagewriter.MessageIcons;
import io.smallrye.common.os.OS;

public enum CodestartType {
    LANGUAGE(true, 1, MessageIcons.toEmoji("U+1F4DA")),
    BUILDTOOL(true, 2, MessageIcons.toEmoji("U+1F528")),
    PROJECT(true, 3, MessageIcons.toEmoji("U+1F4E6")),
    CONFIG(true, 4, MessageIcons.toEmoji("U+1F4DD")),
    TOOLING(false, 5, MessageIcons.toEmoji("U+1F527")),
    CODE(false, 6, MessageIcons.toEmoji("U+1F680")),
    ;

    private final boolean base;
    private final int processingOrder;
    private final String icon;

    CodestartType(boolean base, int processingOrder, String icon) {
        this.base = base;
        this.processingOrder = processingOrder;
        this.icon = icon;
    }

    public boolean isBase() {
        return base;
    }

    public String getIcon() {
        return OS.WINDOWS.isCurrent() ? ">>" : icon;
    }

    public int getProcessingOrder() {
        return processingOrder;
    }

}
