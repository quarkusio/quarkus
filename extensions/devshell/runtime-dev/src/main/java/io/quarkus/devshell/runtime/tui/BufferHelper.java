package io.quarkus.devshell.runtime.tui;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;

/**
 * Helper for writing to a TamboUI Buffer.
 * Provides a cursor-based API similar to terminal output but targeting a buffer.
 * All positions are 0-based (buffer coordinates).
 */
public class BufferHelper {

    private final Buffer buffer;
    private int cursorRow = 0;
    private int cursorCol = 0;

    public BufferHelper(Buffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Move cursor to the given 0-based position.
     */
    public void moveTo(int row, int col) {
        this.cursorRow = Math.max(0, row);
        this.cursorCol = Math.max(0, col);
    }

    /**
     * Write plain text at current cursor position. Advances cursor.
     */
    public void write(String text, Style style) {
        if (text == null || text.isEmpty()) {
            return;
        }
        buffer.setString(cursorCol, cursorRow, text, style != null ? style : Style.EMPTY);
        cursorCol += text.length();
    }

    /**
     * Write plain text at current cursor position with default style.
     */
    public void write(String text) {
        write(text, Style.EMPTY);
    }

    /**
     * Write styled text at a specific 0-based position.
     */
    public static void writeAt(Buffer buffer, int col, int row, String text, Style style) {
        if (text != null && !text.isEmpty()) {
            buffer.setString(col, row, text, style != null ? style : Style.EMPTY);
        }
    }

    /**
     * Write a full-width header bar (blue background, white text, centered).
     */
    public static void writeHeader(Buffer buffer, String title, int width) {
        int padding = (width - title.length()) / 2;
        String headerLine = " ".repeat(Math.max(0, padding)) + title
                + " ".repeat(Math.max(0, width - padding - title.length()));
        buffer.setString(0, 0, headerLine, Style.create().white().onBlue().bold());
    }

    /**
     * Write a horizontal line.
     */
    public static void writeLine(Buffer buffer, int col, int row, int width, Style style) {
        buffer.setString(col, row, "\u2500".repeat(width), style != null ? style : Style.create().gray());
    }

    /**
     * Write a vertical divider.
     */
    public static void writeDivider(Buffer buffer, int col, int startRow, int endRow) {
        Style style = Style.create().gray();
        for (int row = startRow; row < endRow; row++) {
            buffer.setString(col, row, "\u2502", style);
        }
    }

    /**
     * Get the underlying buffer.
     */
    public Buffer getBuffer() {
        return buffer;
    }

    public int getCursorRow() {
        return cursorRow;
    }

    public int getCursorCol() {
        return cursorCol;
    }
}
