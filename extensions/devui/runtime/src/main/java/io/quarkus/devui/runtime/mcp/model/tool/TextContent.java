package io.quarkus.devui.runtime.mcp.model.tool;

public class TextContent {
    private String type = "text";
    private String text;

    public TextContent() {

    }

    public TextContent(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
