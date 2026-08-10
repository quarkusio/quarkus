package io.quarkus.devshell.deployment.tui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.AnsiRenderer;
import io.quarkus.devshell.deployment.tui.KeyCode;

public class ListView<T> {

    private final List<T> items = new ArrayList<>();
    private final Function<T, String> labelExtractor;
    private final Function<T, Style> styleExtractor;

    private static final Style STYLE_SELECTED = Style.EMPTY.reversed();
    private static final Style STYLE_SCROLL_INDICATOR = Style.EMPTY.gray();

    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private int visibleRows = 10;
    private int width = 30;

    public ListView(Function<T, String> labelExtractor) {
        this(labelExtractor, null);
    }

    public ListView(Function<T, String> labelExtractor, Function<T, Style> styleExtractor) {
        this.labelExtractor = labelExtractor;
        this.styleExtractor = styleExtractor;
    }

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

    public T getSelectedItem() {
        if (items.isEmpty() || selectedIndex < 0 || selectedIndex >= items.size()) {
            return null;
        }
        return items.get(selectedIndex);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setVisibleRows(int rows) {
        this.visibleRows = rows;
        adjustScroll();
    }

    public void setWidth(int width) {
        this.width = width;
    }

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

    public void render(Buffer buffer, int startRow, int startCol) {
        for (int i = 0; i < visibleRows; i++) {
            int itemIndex = scrollOffset + i;
            int row = startRow + i;

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

        if (items.size() > visibleRows) {
            int maxOffset = Math.max(1, items.size() - visibleRows);
            if (scrollOffset > 0) {
                buffer.setString(startCol + width - 1, startRow, "▲", STYLE_SCROLL_INDICATOR);
            }
            if (scrollOffset < maxOffset) {
                buffer.setString(startCol + width - 1, startRow + visibleRows - 1, "▼", STYLE_SCROLL_INDICATOR);
            }
        }
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
