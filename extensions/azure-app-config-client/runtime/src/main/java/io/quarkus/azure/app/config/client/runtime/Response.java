package io.quarkus.azure.app.config.client.runtime;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

    private List<Item> items;

    @JsonCreator
    public Response(@JsonProperty("items") List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {

        private String key;
        private String label;
        private String contentType;
        private String value;

        @JsonCreator
        public Item(@JsonProperty("key") String key, @JsonProperty("label") String label,
                @JsonProperty("contentType") String contentType, @JsonProperty("value") String value) {
            this.key = key;
            this.label = label;
            this.contentType = contentType;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
