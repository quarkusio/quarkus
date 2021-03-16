package io.quarkus.devtools.codestarts;

import io.smallrye.common.os.OS;

public enum CodestartType {
    LANGUAGE(true, 1, "\uD83D\uDD20"),
    BUILDTOOL(true, 2, "\uD83E\uDDF0"),
    PROJECT(true, 3, "\uD83D\uDDC3"),
    CONFIG(true, 4, "\uD83D\uDCDC"),
    TOOLING(false, 5, "\uD83D\uDEE0"),
    CODE(false, 6, "\uD83D\uDC12"),
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
