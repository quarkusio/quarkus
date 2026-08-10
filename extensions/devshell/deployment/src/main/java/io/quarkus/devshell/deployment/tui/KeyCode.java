package io.quarkus.devshell.deployment.tui;

/**
 * Key code constants and utilities for keyboard input handling.
 */
public final class KeyCode {

    private KeyCode() {
    }

    public static final int ENTER = '\r';
    public static final int NEWLINE = '\n';
    public static final int ESCAPE = 27;
    public static final int BACKSPACE = 127;
    public static final int TAB = '\t';
    public static final int SPACE = ' ';

    public static final int UP = -1;
    public static final int DOWN = -2;
    public static final int RIGHT = -3;
    public static final int LEFT = -4;
    public static final int HOME = -5;
    public static final int END = -6;
    public static final int PAGE_UP = -7;
    public static final int PAGE_DOWN = -8;
    public static final int DELETE = -9;

    public static final int CTRL_S = 19;

    public static int parse(int[] keys) {
        if (keys == null || keys.length == 0) {
            return -100;
        }

        if (keys.length == 1) {
            return keys[0];
        }

        if (keys.length >= 3 && keys[0] == ESCAPE && keys[1] == '[') {
            return parseEscapeSequence(keys);
        }

        return keys[0];
    }

    private static int parseEscapeSequence(int[] keys) {
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
                if (keys.length >= 4 && keys[3] == '~')
                    return PAGE_UP;
                break;
            case '6':
                if (keys.length >= 4 && keys[3] == '~')
                    return PAGE_DOWN;
                break;
            case '3':
                if (keys.length >= 4 && keys[3] == '~')
                    return DELETE;
                break;
            case '1':
                if (keys.length >= 4 && keys[3] == '~')
                    return HOME;
                break;
            case '4':
                if (keys.length >= 4 && keys[3] == '~')
                    return END;
                break;
        }
        return keys[0];
    }

    public static boolean isPrintable(int key) {
        return key >= 32 && key < 127;
    }
}
