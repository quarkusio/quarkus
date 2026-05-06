package io.quarkus.quickcli;

/**
 * Specification for usage/help message formatting. Stores headings,
 * widths, and other formatting metadata from {@code @Command}.
 */
public final class UsageMessageSpec {

    /** Section key for the command list in the help section map. */
    public static final String SECTION_KEY_COMMAND_LIST = "commandList";

    private String[] header = {};
    private String[] description = {};
    private String[] footer = {};
    private String synopsisHeading = "Usage: ";
    private String commandListHeading = "Commands:%n";
    private String optionListHeading = "Options:%n";
    private String parameterListHeading = "%n";
    private String headerHeading = "";
    private boolean showDefaultValues;
    private int width = 80;
    private boolean adjustLineBreaksForWideCJKCharacters;

    public String[] header() {
        return header;
    }

    public UsageMessageSpec header(String... header) {
        this.header = header;
        return this;
    }

    public String[] description() {
        return description;
    }

    public UsageMessageSpec description(String... description) {
        this.description = description;
        return this;
    }

    public String[] footer() {
        return footer;
    }

    public UsageMessageSpec footer(String... footer) {
        this.footer = footer;
        return this;
    }

    public String synopsisHeading() {
        return synopsisHeading;
    }

    public UsageMessageSpec synopsisHeading(String synopsisHeading) {
        this.synopsisHeading = synopsisHeading;
        return this;
    }

    public String commandListHeading() {
        return commandListHeading;
    }

    public UsageMessageSpec commandListHeading(String commandListHeading) {
        this.commandListHeading = commandListHeading;
        return this;
    }

    public String optionListHeading() {
        return optionListHeading;
    }

    public UsageMessageSpec optionListHeading(String optionListHeading) {
        this.optionListHeading = optionListHeading;
        return this;
    }

    public String parameterListHeading() {
        return parameterListHeading;
    }

    public UsageMessageSpec parameterListHeading(String parameterListHeading) {
        this.parameterListHeading = parameterListHeading;
        return this;
    }

    public String headerHeading() {
        return headerHeading;
    }

    public UsageMessageSpec headerHeading(String headerHeading) {
        this.headerHeading = headerHeading;
        return this;
    }

    public boolean showDefaultValues() {
        return showDefaultValues;
    }

    public UsageMessageSpec showDefaultValues(boolean showDefaultValues) {
        this.showDefaultValues = showDefaultValues;
        return this;
    }

    public int width() {
        return width;
    }

    public UsageMessageSpec width(int width) {
        this.width = width;
        return this;
    }

    public boolean adjustLineBreaksForWideCJKCharacters() {
        return adjustLineBreaksForWideCJKCharacters;
    }

    public UsageMessageSpec adjustLineBreaksForWideCJKCharacters(boolean adjust) {
        this.adjustLineBreaksForWideCJKCharacters = adjust;
        return this;
    }
}
