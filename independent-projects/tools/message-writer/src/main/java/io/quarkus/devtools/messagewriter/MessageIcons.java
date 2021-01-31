package io.quarkus.devtools.messagewriter;

import io.smallrye.common.os.OS;

public enum MessageIcons {

    OK_ICON("\u2705", "[SUCCESS]"),
    NOK_ICON("\u274c", "[FAILURE]"),
    NOOP_ICON("\uD83D\uDC4D", ""),
    WARN_ICON("\uD83D\uDD25", "[WARN]"),
    ERROR_ICON("\u2757", "[ERROR]");

    private String icon;
    private String messageCode;

    MessageIcons(String icon, String messageCode) {
        this.icon = icon;
        this.messageCode = messageCode;
    }

    @Override
    public String toString() {
        return OS.WINDOWS.isCurrent() ? messageCode : String.format("%s %s", messageCode, icon);
    }
}
