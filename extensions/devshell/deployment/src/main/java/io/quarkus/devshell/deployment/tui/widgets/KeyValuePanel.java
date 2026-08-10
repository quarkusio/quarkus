package io.quarkus.devshell.deployment.tui.widgets;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;

public class KeyValuePanel {

    private final String title;
    private final List<Entry> entries = new ArrayList<>();

    public KeyValuePanel(String title) {
        this.title = title;
    }

    public KeyValuePanel() {
        this.title = null;
    }

    public KeyValuePanel add(String key, String value) {
        entries.add(new Entry(key, value != null ? value : "", null));
        return this;
    }

    public KeyValuePanel addIfPresent(String key, String value) {
        if (value != null && !value.isEmpty()) {
            entries.add(new Entry(key, value, null));
        }
        return this;
    }

    public KeyValuePanel addStyled(String key, String value, Style valueStyle) {
        entries.add(new Entry(key, value != null ? value : "", valueStyle));
        return this;
    }

    public KeyValuePanel addBlank() {
        entries.add(new Entry(null, null, null));
        return this;
    }

    public int render(Buffer buffer, int startRow, int startCol, int width) {
        int row = startRow;

        Style headerStyle = Style.EMPTY.cyan().bold();
        Style lineStyle = Style.EMPTY.gray();
        Style labelStyle = Style.EMPTY.cyan();
        Style valueStyle = Style.EMPTY.white();

        if (title != null) {
            buffer.setString(startCol, row, title, headerStyle);
            row++;

            buffer.setString(startCol, row, "─".repeat(width), lineStyle);
            row++;
        }

        for (Entry entry : entries) {
            if (entry.key == null) {
                row++;
                continue;
            }
            String label = entry.key + ": ";
            buffer.setString(startCol, row, label, labelStyle);
            Style vStyle = entry.valueStyle != null ? entry.valueStyle : valueStyle;
            buffer.setString(startCol + label.length(), row, entry.value, vStyle);
            row++;
        }

        return row;
    }

    public void clear() {
        entries.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private static class Entry {
        final String key;
        final String value;
        final Style valueStyle;

        Entry(String key, String value, Style valueStyle) {
            this.key = key;
            this.value = value;
            this.valueStyle = valueStyle;
        }
    }
}
