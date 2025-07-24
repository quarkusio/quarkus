package io.quarkus.devui.runtime.mcp.model.tool;

import java.util.List;

public class CallToolResult {
    private List<TextContent> content;
    private Boolean isError = false;

    public CallToolResult() {

    }

    public CallToolResult(String text) {
        this.content = List.of(new TextContent(text));
    }

    public List<TextContent> getContent() {
        return content;
    }

    public void setContent(List<TextContent> content) {
        this.content = content;
    }

    public Boolean getIsError() {
        return isError;
    }

    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
}
