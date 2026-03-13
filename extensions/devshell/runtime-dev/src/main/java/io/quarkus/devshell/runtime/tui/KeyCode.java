package io.quarkus.devshell.runtime.tui;

/**
 * Key code constants and utilities for keyboard input handling.
 */
public final class KeyCode {

    private KeyCode() {
    }

    // Special keys
    public static final int ENTER = '\r';
    public static final int NEWLINE = '\n';
    public static final int ESCAPE = 27;
    public static final int BACKSPACE = 127;
    public static final int TAB = '\t';
    public static final int SPACE = ' ';

    // Arrow key constants (after parsing escape sequences)
    public static final int UP = -1;
    public static final int DOWN = -2;
    public static final int RIGHT = -3;
    public static final int LEFT = -4;
    public static final int HOME = -5;
    public static final int END = -6;
    public static final int PAGE_UP = -7;
    public static final int PAGE_DOWN = -8;
    public static final int DELETE = -9;

    /**
     * Parse raw key input and return a normalized key code.
     * Handles escape sequences for arrow keys and other special keys.
     *
     * @param keys raw key input as int array
     * @return normalized key code, or the first character for simple keys
     */
    public static int parse(int[] keys) {
        if (keys == null || keys.length == 0) {
            return -100; // Invalid
        }

        // Single character input
        if (keys.length == 1) {
            return keys[0];
        }

        // Escape sequence: ESC [ <code>
        if (keys.length >= 3 && keys[0] == ESCAPE && keys[1] == '[') {
            return parseEscapeSequence(keys);
        }

        // Multi-char input that isn't a recognized escape sequence
        return keys[0];
    }

    private static int parseEscapeSequence(int[] keys) {
        // ESC [ A = Up, ESC [ B = Down, ESC [ C = Right, ESC [ D = Left
        switch (keys[2]) {
            case 'A':
                return UP;
            case 'B':
                return DOWN;
            case 'C':
                return RIGHT;
            case 'D':
                return LEFT;
            case 'H':
                return HOME;
            case 'F':
                return END;
            case '5':
                // ESC [ 5 ~ = Page Up
                if (keys.length >= 4 && keys[3] == '~') {
                    return PAGE_UP;
                }
                break;
            case '6':
                // ESC [ 6 ~ = Page Down
                if (keys.length >= 4 && keys[3] == '~') {
                    return PAGE_DOWN;
                }
                break;
            case '3':
                // ESC [ 3 ~ = Delete
                if (keys.length >= 4 && keys[3] == '~') {
                    return DELETE;
                }
                break;
            case '1':
                // ESC [ 1 ~ = Home (some terminals)
                if (keys.length >= 4 && keys[3] == '~') {
                    return HOME;
                }
                break;
            case '4':
                // ESC [ 4 ~ = End (some terminals)
                if (keys.length >= 4 && keys[3] == '~') {
                    return END;
                }
                break;
        }

        return keys[0]; // Fallback
    }

    /**
     * Check if key is a printable character.
     */
    public static boolean isPrintable(int key) {
        return key >= 32 && key < 127;
    }

    /**
     * Check if key is a letter (a-z, A-Z).
     */
    public static boolean isLetter(int key) {
        return (key >= 'a' && key <= 'z') || (key >= 'A' && key <= 'Z');
    }

    /**
     * Check if key is a digit (0-9).
     */
    public static boolean isDigit(int key) {
        return key >= '0' && key <= '9';
    }

    /**
     * Convert key to uppercase if it's a letter.
     */
    public static int toUpperCase(int key) {
        if (key >= 'a' && key <= 'z') {
            return key - 32;
        }
        return key;
    }
}
