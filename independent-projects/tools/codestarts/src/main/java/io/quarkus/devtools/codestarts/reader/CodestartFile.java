package io.quarkus.devtools.codestarts.reader;

public final class CodestartFile {

    private final String sourceFileRelativePath;
    private final String content;

    public CodestartFile(String sourceFileRelativePath, String content) {
        this.sourceFileRelativePath = sourceFileRelativePath;
        this.content = content;
    }

    public String getSourceFileRelativePath() {
        return sourceFileRelativePath;
    }

    public String getContent() {
        return content;
    }

}
