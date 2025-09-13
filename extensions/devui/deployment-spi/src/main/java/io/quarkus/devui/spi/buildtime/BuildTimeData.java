package io.quarkus.devui.spi.buildtime;

/**
 * Holds the actual data and optionally a description for Build Time Data
 */
public class BuildTimeData {
    private Object content;
    private String description;
    private String contentType = "application/json"; // default
    private boolean mcpEnabledAsDefault = false;

    public BuildTimeData() {

    }

    public BuildTimeData(Object content) {
        this(content, null);
    }

    public BuildTimeData(Object content, String description) {
        this.content = content;
        this.description = description;
    }

    public BuildTimeData(Object content, String description, boolean mcpEnabledAsDefault) {
        this.content = content;
        this.description = description;
        this.mcpEnabledAsDefault = mcpEnabledAsDefault;
    }

    public BuildTimeData(Object content, String description, boolean mcpEnabledAsDefault, String contentType) {
        this.content = content;
        this.description = description;
        this.mcpEnabledAsDefault = mcpEnabledAsDefault;
        this.contentType = contentType;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isMcpEnabledAsDefault() {
        return mcpEnabledAsDefault;
    }

    public void setMcpEnabledAsDefault(boolean mcpEnabledAsDefault) {
        this.mcpEnabledAsDefault = mcpEnabledAsDefault;
    }
}