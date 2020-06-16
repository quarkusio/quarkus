package io.quarkus.cli.commands;

public enum ExtensionFormat {
    CONCISE("concise"),
    NAME("name"),
    FULL("full");

    private final String format;

    ExtensionFormat(String format) {
        this.format = format;
    }

    public String formatValue() {
        return format;
    }

    public static ExtensionFormat findFormat(String format) {
        if (format.equalsIgnoreCase(FULL.format))
            return FULL;
        else if (format.equalsIgnoreCase(NAME.format))
            return NAME;
        else
            return CONCISE;
    }
}
