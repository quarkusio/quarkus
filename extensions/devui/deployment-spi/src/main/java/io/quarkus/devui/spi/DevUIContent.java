package io.quarkus.devui.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * Content that is made available in the DEV UI
 */
public class DevUIContent {
    private final String fileName;
    private final byte[] template;
    private final Map<String, Object> data;
    private final Map<String, String> descriptions;
    private final Map<String, String> mcpDefaultEnabled;
    private final Map<String, String> contentTypes;

    private DevUIContent(DevUIContent.Builder builder) {
        this.fileName = builder.fileName;
        this.template = builder.template;
        this.data = builder.data;
        this.descriptions = builder.descriptions;
        this.mcpDefaultEnabled = builder.mcpDefaultEnabled;
        this.contentTypes = builder.contentTypes;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getTemplate() {
        return template;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public Map<String, String> getMcpDefaultEnables() {
        return mcpDefaultEnabled;
    }

    public Map<String, String> getContentTypes() {
        return contentTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fileName;
        private byte[] template;
        private Map<String, Object> data;
        private Map<String, String> descriptions;
        private Map<String, String> contentTypes;
        private Map<String, String> mcpDefaultEnabled;

        private Builder() {
            this.data = new HashMap<>();
        }

        public Builder fileName(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                throw new RuntimeException("Invalid fileName");
            }
            this.fileName = fileName;
            return this;
        }

        public Builder template(byte[] template) {
            if (template == null || template.length == 0) {
                throw new RuntimeException("Invalid template");
            }

            this.template = template;
            return this;
        }

        public Builder addData(Map<String, Object> data) {
            this.data.putAll(data);
            return this;
        }

        public Builder addData(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public Builder descriptions(Map<String, String> descriptions) {
            this.descriptions = descriptions;
            return this;
        }

        public Builder mcpDefaultEnables(Map<String, String> mcpDefaultEnabled) {
            this.mcpDefaultEnabled = mcpDefaultEnabled;
            return this;
        }

        public Builder contentTypes(Map<String, String> contentTypes) {
            this.contentTypes = contentTypes;
            return this;
        }

        public DevUIContent build() {
            if (fileName == null) {
                throw new RuntimeException(
                        ERROR + " FileName is mandatory, for example 'index.html'");
            }

            if (template == null) {
                template = DEFAULT_TEMPLATE;
            }

            return new DevUIContent(this);
        }

        private static final String ERROR = "Not enough information to create Dev UI content.";
        private static final byte[] DEFAULT_TEMPLATE = "Here the template of your page. Set your own by providing the template() in the DevUIContent"
                .getBytes();
    }
}
