package io.quarkus.devshell.runtime.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for shell page content.
 * Providers return this to describe what should be displayed.
 */
public final class ShellPageData {

    private final List<Section> sections;
    private final String error;

    private ShellPageData(List<Section> sections, String error) {
        this.sections = sections;
        this.error = error;
    }

    public List<Section> getSections() {
        return sections;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }

    /**
     * Create a builder for constructing page data.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create an error result.
     */
    public static ShellPageData error(String message) {
        return new ShellPageData(List.of(), message);
    }

    /**
     * Create an empty result.
     */
    public static ShellPageData empty() {
        return new ShellPageData(List.of(), null);
    }

    /**
     * A section of content with a title and items.
     */
    public static final class Section {
        private final String title;
        private final List<Item> items;

        public Section(String title, List<Item> items) {
            this.title = title;
            this.items = items != null ? items : List.of();
        }

        public String getTitle() {
            return title;
        }

        public List<Item> getItems() {
            return items;
        }
    }

    /**
     * An item to display - a label/value pair with optional styling.
     */
    public static final class Item {
        private final String label;
        private final String value;
        private final ShellPageProvider.ItemStyle style;

        public Item(String label, String value) {
            this(label, value, ShellPageProvider.ItemStyle.TEXT);
        }

        public Item(String label, String value, ShellPageProvider.ItemStyle style) {
            this.label = label;
            this.value = value;
            this.style = style != null ? style : ShellPageProvider.ItemStyle.TEXT;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public ShellPageProvider.ItemStyle getStyle() {
            return style;
        }

        /**
         * Create a header item (no value, just a styled label).
         */
        public static Item header(String label) {
            return new Item(label, null, ShellPageProvider.ItemStyle.HEADER);
        }

        /**
         * Create a text item.
         */
        public static Item text(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.TEXT);
        }

        /**
         * Create a code/monospace item.
         */
        public static Item code(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.CODE);
        }

        /**
         * Create a success status item.
         */
        public static Item ok(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.STATUS_OK);
        }

        /**
         * Create a warning status item.
         */
        public static Item warning(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.STATUS_WARNING);
        }

        /**
         * Create an error status item.
         */
        public static Item error(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.STATUS_ERROR);
        }
    }

    /**
     * Builder for constructing ShellPageData.
     */
    public static final class Builder {
        private final List<Section> sections = new ArrayList<>();

        public Builder addSection(String title, List<Item> items) {
            sections.add(new Section(title, items));
            return this;
        }

        public Builder addSection(Section section) {
            sections.add(section);
            return this;
        }

        public ShellPageData build() {
            return new ShellPageData(List.copyOf(sections), null);
        }
    }
}
