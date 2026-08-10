package io.quarkus.devshell.deployment.tui;

import java.util.Optional;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

/**
 * Writes a TamboUI Buffer to a string of ANSI escape sequences.
 * Uses only SGR codes compatible with aesh's Connection.write().
 */
final class AnsiWriter {

    private static final char ESC = '';

    private AnsiWriter() {
    }

    static String fullRender(Buffer buffer) {
        StringBuilder sb = new StringBuilder(buffer.width() * buffer.height() * 4);
        Rect area = buffer.area();
        Style lastStyle = null;

        for (int y = area.y(); y < area.y() + area.height(); y++) {
            cursorTo(sb, y + 1, 1);
            lastStyle = null;

            for (int x = area.x(); x < area.x() + area.width(); x++) {
                Cell cell = buffer.get(x, y);
                Style style = cell.style();

                if (!style.equals(lastStyle)) {
                    appendStyle(sb, style);
                    lastStyle = style;
                }
                sb.append(cell.symbol());
            }
        }
        reset(sb);
        return sb.toString();
    }

    static String diffRender(Buffer current, Buffer previous) {
        StringBuilder sb = new StringBuilder(1024);
        Rect area = current.area();
        Style lastStyle = null;
        int lastX = -2;
        int lastY = -1;

        for (int y = area.y(); y < area.y() + area.height(); y++) {
            for (int x = area.x(); x < area.x() + area.width(); x++) {
                Cell cell = current.get(x, y);
                Cell prevCell = previous.get(x, y);

                if (cellEquals(cell, prevCell)) {
                    continue;
                }

                if (y != lastY || x != lastX + 1) {
                    cursorTo(sb, y + 1, x + 1);
                    lastStyle = null;
                }
                lastX = x;
                lastY = y;

                Style style = cell.style();
                if (!style.equals(lastStyle)) {
                    appendStyle(sb, style);
                    lastStyle = style;
                }
                sb.append(cell.symbol());
            }
        }
        if (sb.length() > 0) {
            reset(sb);
        }
        return sb.toString();
    }

    private static boolean cellEquals(Cell a, Cell b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.symbol().equals(b.symbol()) && a.style().equals(b.style());
    }

    private static void appendStyle(StringBuilder sb, Style style) {
        sb.append(ESC).append("[0");

        Optional<Color> fg = style.fg();
        if (fg.isPresent()) {
            int code = colorToSgr(fg.get(), true);
            if (code >= 0) {
                sb.append(';').append(code);
            }
        }

        Optional<Color> bg = style.bg();
        if (bg.isPresent()) {
            int code = colorToSgr(bg.get(), false);
            if (code >= 0) {
                sb.append(';').append(code);
            }
        }

        var mods = style.effectiveModifiers();
        if (mods.contains(Modifier.BOLD)) {
            sb.append(";1");
        }
        if (mods.contains(Modifier.DIM)) {
            sb.append(";2");
        }
        if (mods.contains(Modifier.ITALIC)) {
            sb.append(";3");
        }
        if (mods.contains(Modifier.UNDERLINED)) {
            sb.append(";4");
        }
        if (mods.contains(Modifier.REVERSED)) {
            sb.append(";7");
        }

        sb.append('m');
    }

    private static int colorToSgr(Color color, boolean foreground) {
        int base = foreground ? 30 : 40;

        if (color == Color.BLACK)
            return base;
        if (color == Color.RED)
            return base + 1;
        if (color == Color.GREEN)
            return base + 2;
        if (color == Color.YELLOW)
            return base + 3;
        if (color == Color.BLUE)
            return base + 4;
        if (color == Color.MAGENTA)
            return base + 5;
        if (color == Color.CYAN)
            return base + 6;
        if (color == Color.WHITE)
            return base + 7;

        if (color == Color.GRAY || color == Color.DARK_GRAY)
            return foreground ? 90 : 100;
        if (color == Color.LIGHT_RED)
            return foreground ? 91 : 101;
        if (color == Color.LIGHT_GREEN)
            return foreground ? 92 : 102;
        if (color == Color.LIGHT_YELLOW)
            return foreground ? 93 : 103;
        if (color == Color.LIGHT_BLUE)
            return foreground ? 94 : 104;
        if (color == Color.LIGHT_MAGENTA)
            return foreground ? 95 : 105;
        if (color == Color.LIGHT_CYAN)
            return foreground ? 96 : 106;
        if (color == Color.BRIGHT_WHITE)
            return foreground ? 97 : 107;

        return -1;
    }

    private static void cursorTo(StringBuilder sb, int row, int col) {
        sb.append(ESC).append('[').append(row).append(';').append(col).append('H');
    }

    private static void reset(StringBuilder sb) {
        sb.append(ESC).append("[0m");
    }
}
