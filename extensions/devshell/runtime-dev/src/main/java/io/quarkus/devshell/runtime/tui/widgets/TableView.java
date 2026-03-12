package io.quarkus.devshell.runtime.tui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import io.quarkus.devshell.runtime.tui.AnsiRenderer;
import io.quarkus.devshell.runtime.tui.KeyCode;

/**
 * A scrollable table widget with column headers and selection highlight.
 *
 * @param <T> the type of items in the table
 */
public class TableView<T> {

    private final List<T> items = new ArrayList<>();
    private final List<Column<T>> columns = new ArrayList<>();

    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int visibleRows = 10;
    private int width = 80;

    /**
     * Add a column to the table.
     *
     * @param header the column header
     * @param extractor function to extract cell value from item
     * @param minWidth minimum column width
     * @return this for chaining
     */
    public TableView<T> addColumn(String header, Function<T, String> extractor, int minWidth) {
        columns.add(new Column<>(header, extractor, minWidth));
        return this;
    }

    /**
     * Add a column with default minimum width.
     */
    public TableView<T> addColumn(String header, Function<T, String> extractor) {
        return addColumn(header, extractor, 10);
    }

    /**
     * Set the items to display.
     */
    public void setItems(List<T> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = Math.max(0, this.items.size() - 1);
        }
        adjustScroll();
    }

    /**
     * Get the currently selected item.
     */
    public T getSelectedItem() {
        if (items.isEmpty() || selectedIndex < 0 || selectedIndex >= items.size()) {
            return null;
        }
        return items.get(selectedIndex);
    }

    /**
     * Get the selected index.
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Set the visible rows (viewport height, excluding header).
     */
    public void setVisibleRows(int rows) {
        this.visibleRows = rows;
        adjustScroll();
    }

    /**
     * Set the width of the table.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Handle keyboard input.
     *
     * @return true if key was handled
     */
    public boolean handleKey(int key) {
        switch (key) {
            case KeyCode.UP:
                moveUp();
                return true;
            case KeyCode.DOWN:
                moveDown();
                return true;
            case KeyCode.PAGE_UP:
                pageUp();
                return true;
            case KeyCode.PAGE_DOWN:
                pageDown();
                return true;
            case KeyCode.HOME:
                moveToStart();
                return true;
            case KeyCode.END:
                moveToEnd();
                return true;
            default:
                return false;
        }
    }

    public void moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            adjustScroll();
        }
    }

    public void moveDown() {
        if (selectedIndex < items.size() - 1) {
            selectedIndex++;
            adjustScroll();
        }
    }

    public void pageUp() {
        selectedIndex = Math.max(0, selectedIndex - visibleRows);
        adjustScroll();
    }

    public void pageDown() {
        selectedIndex = Math.min(items.size() - 1, selectedIndex + visibleRows);
        adjustScroll();
    }

    public void moveToStart() {
        selectedIndex = 0;
        adjustScroll();
    }

    public void moveToEnd() {
        selectedIndex = Math.max(0, items.size() - 1);
        adjustScroll();
    }

    private void adjustScroll() {
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }
        scrollOffset = Math.max(0, scrollOffset);
        int maxOffset = Math.max(0, items.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxOffset);
    }

    /**
     * Render the table at the specified position into the buffer.
     *
     * @param buffer the buffer to render into
     * @param startRow starting row (0-based)
     * @param startCol starting column (0-based)
     */
    public void render(Buffer buffer, int startRow, int startCol) {
        if (columns.isEmpty()) {
            return;
        }

        // Calculate column widths
        int[] colWidths = calculateColumnWidths();
        int row = startRow;

        // Render header
        renderHeader(buffer, row, startCol, colWidths);
        row++;

        // Render separator
        renderSeparator(buffer, row, startCol, colWidths);
        row++;

        // Render rows
        for (int i = 0; i < visibleRows; i++) {
            int itemIndex = scrollOffset + i;
            buffer.setString(startCol, row, " ".repeat(width), Style.EMPTY);

            if (itemIndex < items.size()) {
                T item = items.get(itemIndex);
                boolean selected = itemIndex == selectedIndex;
                renderRow(buffer, row, startCol, item, colWidths, selected);
            }
            row++;
        }

        // Show scroll indicators if needed
        if (items.size() > visibleRows) {
            renderScrollIndicator(buffer, startRow + 2, startCol + width - 1);
        }
    }

    private int[] calculateColumnWidths() {
        int[] widths = new int[columns.size()];
        int totalMinWidth = 0;

        // Start with minimum widths
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = Math.max(columns.get(i).minWidth, columns.get(i).header.length() + 2);
            totalMinWidth += widths[i];
        }

        // Distribute remaining space
        int available = width - totalMinWidth - (columns.size() - 1) * 2; // 2 chars gap between columns
        if (available > 0) {
            int extra = available / columns.size();
            for (int i = 0; i < columns.size(); i++) {
                widths[i] += extra;
            }
        }

        return widths;
    }

    private void renderHeader(Buffer buffer, int row, int startCol, int[] colWidths) {
        Style headerStyle = Style.create().cyan().bold();
        int col = startCol;
        for (int i = 0; i < columns.size(); i++) {
            String header = padOrTruncate(columns.get(i).header, colWidths[i]);
            buffer.setString(col, row, header, headerStyle);
            col += colWidths[i] + 2;
        }
    }

    private void renderSeparator(Buffer buffer, int row, int startCol, int[] colWidths) {
        Style sepStyle = Style.create().gray();
        int col = startCol;
        for (int i = 0; i < columns.size(); i++) {
            buffer.setString(col, row, "\u2500".repeat(colWidths[i]), sepStyle);
            col += colWidths[i] + 2;
        }
    }

    private void renderRow(Buffer buffer, int row, int startCol, T item, int[] colWidths, boolean selected) {
        Style rowStyle = selected ? Style.create().reversed() : Style.EMPTY;

        int col = startCol;
        for (int i = 0; i < columns.size(); i++) {
            String value = columns.get(i).extractor.apply(item);
            if (value == null) {
                value = "";
            }
            // Strip ANSI codes for width calculation
            String stripped = AnsiRenderer.stripAnsi(value);
            String padded;
            if (stripped.length() > colWidths[i]) {
                padded = stripped.substring(0, colWidths[i] - 1) + "~";
            } else {
                padded = stripped + " ".repeat(colWidths[i] - stripped.length());
            }

            buffer.setString(col, row, padded, rowStyle);
            col += colWidths[i] + 2;
        }
    }

    private String padOrTruncate(String text, int width) {
        if (text.length() > width) {
            return text.substring(0, width - 1) + "~";
        }
        return text + " ".repeat(width - text.length());
    }

    private void renderScrollIndicator(Buffer buffer, int startRow, int col) {
        int maxOffset = Math.max(1, items.size() - visibleRows);
        Style indicatorStyle = Style.create().gray();

        if (scrollOffset > 0) {
            buffer.setString(col, startRow, "\u25B2", indicatorStyle);
        }

        if (scrollOffset < maxOffset) {
            buffer.setString(col, startRow + visibleRows - 1, "\u25BC", indicatorStyle);
        }
    }

    /**
     * Get the number of items.
     */
    public int size() {
        return items.size();
    }

    /**
     * Check if the table is empty.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Clear all columns.
     */
    public void clearColumns() {
        columns.clear();
    }

    private static class Column<T> {
        final String header;
        final Function<T, String> extractor;
        final int minWidth;

        Column(String header, Function<T, String> extractor, int minWidth) {
            this.header = header;
            this.extractor = extractor;
            this.minWidth = minWidth;
        }
    }
}
