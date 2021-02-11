package io.quarkus.annotation.processor.generate_doc;

import java.util.List;
import java.util.Objects;

import io.quarkus.annotation.processor.Constants;

public class ConfigDocGeneratedOutput {
    private final String fileName;
    private final boolean searchable;
    private final boolean hasAnchorPrefix;
    private final List<ConfigDocItem> configDocItems;

    public ConfigDocGeneratedOutput(String fileName, boolean searchable, List<ConfigDocItem> configDocItems,
            boolean hasAnchorPrefix) {
        this.fileName = fileName;
        this.searchable = searchable;
        this.configDocItems = configDocItems;
        this.hasAnchorPrefix = hasAnchorPrefix;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public List<ConfigDocItem> getConfigDocItems() {
        return configDocItems;
    }

    public String getAnchorPrefix() {
        if (!hasAnchorPrefix) {
            return Constants.EMPTY;
        }

        String anchorPrefix = fileName;
        if (fileName.endsWith(Constants.ADOC_EXTENSION)) {
            anchorPrefix = anchorPrefix.substring(0, anchorPrefix.length() - 5);
        }

        return anchorPrefix + "_";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConfigDocGeneratedOutput that = (ConfigDocGeneratedOutput) o;
        return Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName);
    }

    @Override
    public String toString() {
        return "ConfigItemsOutput{" +
                "fileName='" + fileName + '\'' +
                ", searchable=" + searchable +
                ", configDocItems=" + configDocItems +
                '}';
    }
}
