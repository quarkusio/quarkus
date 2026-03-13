package io.quarkus.devshell.runtime.tui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import io.quarkus.devshell.runtime.tui.AnsiRenderer;
import io.quarkus.devshell.runtime.tui.KeyCode;

/**
 * A scrollable list widget with selection highlight.
 *
 * @param <T> the type of items in the list
 */
public class ListView<T> {

    private final List<T> items = new ArrayList<>();
    private final Function<T, String> labelExtractor;
    private final Function<T, Style> styleExtractor;

    private static final Style STYLE_SELECTED = Style.create().reversed();
    private static final Style STYLE_SCROLL_INDICATOR = Style.create().gray();

    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int visibleRows = 10;
    private int width = 30;

    /**
     * Create a list view with a label extractor function.
     *
     * @param labelExtractor function to extract display label from items
     */
    public ListView(Function<T, String> labelExtractor) {
        this(labelExtractor, null);
    }

    /**
     * Create a list view with a label extractor and a per-item style extractor.
     *
     * @param labelExtractor function to extract display label from items
     * @param styleExtractor function to extract style from items, or null for default
     */
    public ListView(Function<T, String> labelExtractor, Function<T, Style> styleExtractor) {
        this.labelExtractor = labelExtractor;
        this.styleExtractor = styleExtractor;
    }

    /**
     * Set the items to display.
     */
    public void setItems(List<T> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        // Reset selection if needed
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
     * Set the visible rows (viewport height).
     */
    public void setVisibleRows(int rows) {
        this.visibleRows = rows;
        adjustScroll();
    }

    /**
     * Set the width of the list.
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

    /**
     * Move selection up.
     */
    public void moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            adjustScroll();
        }
    }

    /**
     * Move selection down.
     */
    public void moveDown() {
        if (selectedIndex < items.size() - 1) {
            selectedIndex++;
            adjustScroll();
        }
    }

    /**
     * Page up.
     */
    public void pageUp() {
        selectedIndex = Math.max(0, selectedIndex - visibleRows);
        adjustScroll();
    }

    /**
     * Page down.
     */
    public void pageDown() {
        selectedIndex = Math.min(items.size() - 1, selectedIndex + visibleRows);
        adjustScroll();
    }

    /**
     * Move to start.
     */
    public void moveToStart() {
        selectedIndex = 0;
        adjustScroll();
    }

    /**
     * Move to end.
     */
    public void moveToEnd() {
        selectedIndex = Math.max(0, items.size() - 1);
        adjustScroll();
    }

    private void adjustScroll() {
        // Ensure selected item is visible
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        // Clamp scroll offset
        scrollOffset = Math.max(0, scrollOffset);
        int maxOffset = Math.max(0, items.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxOffset);
    }

    /**
     * Render the list at the specified position into the buffer.
     *
     * @param buffer the buffer to render into
     * @param startRow starting row (0-based)
     * @param startCol starting column (0-based)
     */
    public void render(Buffer buffer, int startRow, int startCol) {
        for (int i = 0; i < visibleRows; i++) {
            int itemIndex = scrollOffset + i;
            int row = startRow + i;

            // Clear area
            buffer.setString(startCol, row, " ".repeat(width), Style.EMPTY);

            if (itemIndex < items.size()) {
                T item = items.get(itemIndex);
                String label = labelExtractor.apply(item);
                String stripped = AnsiRenderer.stripAnsi(label);
                boolean selected = itemIndex == selectedIndex;
                Style itemStyle = styleExtractor != null ? styleExtractor.apply(item) : Style.EMPTY;

                if (selected) {
                    String text = AnsiRenderer.fixedWidth(stripped, width - 4);
                    buffer.setString(startCol, row, AnsiRenderer.ARROW_RIGHT + " ", Style.EMPTY);
                    buffer.setString(startCol + 2, row, text, STYLE_SELECTED);
                } else {
                    String text = AnsiRenderer.fixedWidth(stripped, width - 4);
                    buffer.setString(startCol, row, "  " + text, itemStyle);
                }
            }
        }

        // Show scroll indicators if needed
        if (items.size() > visibleRows) {
            renderScrollIndicator(buffer, startRow, startCol + width - 1);
        }
    }

    private void renderScrollIndicator(Buffer buffer, int startRow, int col) {
        int maxOffset = Math.max(1, items.size() - visibleRows);

        if (scrollOffset > 0) {
            buffer.setString(col, startRow, "\u25B2", STYLE_SCROLL_INDICATOR);
        }

        if (scrollOffset < maxOffset) {
            buffer.setString(col, startRow + visibleRows - 1, "\u25BC", STYLE_SCROLL_INDICATOR);
        }
    }

    /**
     * Get the number of items.
     */
    public int size() {
        return items.size();
    }

    /**
     * Check if the list is empty.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}
