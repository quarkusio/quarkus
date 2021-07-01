package io.quarkus.deployment.dev.testing;

public class MessageFormat {

    public static final String RED = "\u001B[91m";
    public static final String GREEN = "\u001b[32m";
    public static final String BLUE = "\u001b[34m";
    public static final String RESET = "\u001b[39m";

    private MessageFormat() {
    }

    public static String statusHeader(String header) {
        return RESET + "==================== " + header + RESET + " ====================";
    }

    public static String statusFooter(String footer) {
        return RESET + ">>>>>>>>>>>>>>>>>>>> " + footer + RESET + " <<<<<<<<<<<<<<<<<<<<";
    }

    public static String toggleStatus(boolean enabled) {
        return " (" + (enabled ? GREEN + "enabled" + RESET + "" : RED + "disabled") + RESET + ")";
    }

    public static String helpOption(String key, String description) {
        return "[" + BLUE + key + RESET + "] - " + description;
    }

    public static String helpOption(String key, String description, boolean enabled) {
        return helpOption(key, description) + toggleStatus(enabled);
    }

}
