package io.quarkus.devtools.codestarts.core.reader;

public final class TargetFile {

    private final String sourceName;
    private final String content;

    public TargetFile(String sourceName, String content) {
        this.sourceName = sourceName;
        this.content = content;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getContent() {
        return content;
    }

}
