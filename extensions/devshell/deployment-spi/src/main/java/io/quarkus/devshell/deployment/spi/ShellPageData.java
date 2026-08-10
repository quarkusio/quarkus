package io.quarkus.devshell.deployment.spi;

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

    public static Builder builder() {
        return new Builder();
    }

    public static ShellPageData error(String message) {
        return new ShellPageData(List.of(), message);
    }

    public static ShellPageData empty() {
        return new ShellPageData(List.of(), null);
    }

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

        public static Item header(String label) {
            return new Item(label, null, ShellPageProvider.ItemStyle.HEADER);
        }

        public static Item text(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.TEXT);
        }

        public static Item code(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.CODE);
        }

        public static Item ok(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.STATUS_OK);
        }

        public static Item warning(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.STATUS_WARNING);
        }

        public static Item error(String label, String value) {
            return new Item(label, value, ShellPageProvider.ItemStyle.STATUS_ERROR);
        }
    }

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
