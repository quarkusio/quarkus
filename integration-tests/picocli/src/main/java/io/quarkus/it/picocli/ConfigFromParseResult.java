package io.quarkus.it.picocli;

public class ConfigFromParseResult {
    private final String parsedName;

    public ConfigFromParseResult(String parsedName) {
        this.parsedName = parsedName;
    }

    public String getParsedName() {
        return parsedName;
    }
}
