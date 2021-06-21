package io.quarkus.devtools.codestarts;

import io.smallrye.common.os.OS;

public enum CodestartType {
    LANGUAGE(true, 1, toEmoji("U+1F4DA")),
    BUILDTOOL(true, 2, toEmoji("U+1F528")),
    PROJECT(true, 3, toEmoji("U+1F4E6")),
    CONFIG(true, 4, toEmoji("U+1F4DD")),
    TOOLING(false, 5, toEmoji("U+1F527")),
    CODE(false, 6, toEmoji("U+1F4C4")),
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

    // Simplify life so we can just understand what emojis we are using
    static String toEmoji(String text) {
        String[] codes = text.replace("U+", "0x").split(" ");
        final StringBuilder stringBuilder = new StringBuilder();
        for (String code : codes) {
            final Integer intCode = Integer.decode(code.trim());
            for (Character character : Character.toChars(intCode)) {
                stringBuilder.append(character);
            }
        }
        stringBuilder.append(' ');
        return stringBuilder.toString();
    }
}
