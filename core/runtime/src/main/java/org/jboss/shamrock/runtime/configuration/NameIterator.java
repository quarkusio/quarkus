package io.quarkus.runtime.configuration;

import java.util.NoSuchElementException;

import org.wildfly.common.Assert;

/**
 */
public final class NameIterator {
    /**
     * Configuration key maximum allowed length.
     */
    public static final int MAX_LENGTH = 2048;
    private static final int POS_MASK = 0x0FFF;
    private static final int POS_BITS = 12;
    private static final int SE_SHIFT = 32 - POS_BITS;

    private final String name;
    private int pos;

    public NameIterator(final String name) {
        this(name, false);
    }

    public NameIterator(final String name, final boolean startAtEnd) {
        this(name, startAtEnd ? name.length() : -1);
    }

    public NameIterator(final String name, final int pos) {
        Assert.checkNotNullParam("name", name);
        if (name.length() > MAX_LENGTH)
            throw new IllegalArgumentException("Name is too long");
        Assert.checkMinimumParameter("pos", -1, pos);
        Assert.checkMaximumParameter("pos", name.length(), pos);
        if (pos != -1 && pos != name.length() && name.charAt(pos) != '.')
            throw new IllegalArgumentException("Position is not located at a delimiter");
        this.name = name;
        this.pos = pos;
    }

    public void goToEnd() {
        this.pos = name.length();
    }

    public void goToStart() {
        this.pos = -1;
    }

    /**
     * Get the cursor position. It will be {@code -1} if the cursor is at the beginning of the string, or {@code name.length()}
     * if it is at the end.
     *
     * @return the cursor position
     */
    public int getPosition() {
        return pos;
    }

    /*
     * next-iteration DFA
     * <any> → <end> ## on EOI
     * I → <end> ## on '.'
     * I → Q ## on '"'
     * Q → I ## on '"'
     * Q → QBS ## on '\'
     * QBS → Q ## on any
     * I → BS ## on '\'
     * BS → I ## on any
     */
    private static final int FS_INITIAL = 0;
    private static final int FS_QUOTE = 1;
    private static final int FS_BACKSLASH = 2;
    private static final int FS_QUOTE_BACKSLASH = 3;

    /*
     * Iteration cookie format
     *
     * Bit: 14...12 11 ... 0
     * ┌───────┬────────────┐
     * │ state │ position │
     * │ │ (signed) │
     * └───────┴────────────┘
     */

    /**
     * Create a new iteration cookie at the current position.
     *
     * @return the new cookie
     */
    private int initIteration() {
        return this.pos & POS_MASK;
    }

    private int cookieOf(int state, int pos) {
        return state << POS_BITS | pos & POS_MASK;
    }

    private int getPosition(int cookie) {
        return (cookie & POS_MASK) << SE_SHIFT >> SE_SHIFT;
    }

    private int getState(int cookie) {
        return cookie >> POS_BITS;
    }

    /**
     * Move to the next position.
     *
     * @param cookie the original cookie value
     * @return the new cookie value
     */
    private int nextPos(int cookie) {
        int pos = getPosition(cookie);
        if (isEndOfString(cookie)) {
            throw new NoSuchElementException();
        }
        int state = getState(cookie);
        int ch;
        for (;;) {
            pos++;
            if (pos == name.length()) {
                return cookieOf(state, pos);
            }
            ch = name.charAt(pos);
            if (state == FS_INITIAL) {
                if (ch == '.') {
                    return cookieOf(state, pos);
                } else if (ch == '"') {
                    state = FS_QUOTE;
                } else if (ch == '\\') {
                    state = FS_BACKSLASH;
                } else {
                    return cookieOf(state, pos);
                }
            } else if (state == FS_QUOTE) {
                if (ch == '"') {
                    state = FS_INITIAL;
                } else if (ch == '\\') {
                    state = FS_QUOTE_BACKSLASH;
                } else {
                    return cookieOf(state, pos);
                }
            } else if (state == FS_BACKSLASH) {
                state = FS_INITIAL;
                return cookieOf(state, pos);
            } else {
                assert state == FS_QUOTE_BACKSLASH;
                state = FS_QUOTE;
                return cookieOf(state, pos);
            }
        }
    }

    private int prevPos(int cookie) {
        int pos = getPosition(cookie);
        if (isStartOfString(cookie)) {
            throw new NoSuchElementException();
        }
        int state = getState(cookie);
        int ch;
        for (;;) {
            pos--;
            if (pos == -1) {
                return cookieOf(state, pos);
            }
            ch = name.charAt(pos);
            if (state == FS_INITIAL) {
                if (pos >= 1 && name.charAt(pos - 1) == '\\') {
                    // always accept as-is
                    return cookieOf(state, pos);
                } else if (ch == '.') {
                    return cookieOf(state, pos);
                } else if (ch == '"') {
                    state = FS_QUOTE;
                } else if (ch == '\\') {
                    // skip
                } else {
                    // regular char
                    return cookieOf(state, pos);
                }
            } else if (state == FS_QUOTE) {
                if (pos >= 1 && name.charAt(pos - 1) == '\\') {
                    // always accept as-is
                    return cookieOf(state, pos);
                } else if (ch == '"') {
                    state = FS_INITIAL;
                } else if (ch == '\\') {
                    // skip
                } else {
                    return cookieOf(state, pos);
                }
            } else {
                throw Assert.unreachableCode();
            }
        }
    }

    private boolean isSegmentDelimiter(int cookie) {
        return isStartOfString(cookie) || isEndOfString(cookie) || getState(cookie) == FS_INITIAL && charAt(cookie) == '.';
    }

    private boolean isEndOfString(int cookie) {
        return getPosition(cookie) == name.length();
    }

    private boolean isStartOfString(int cookie) {
        return getPosition(cookie) == -1;
    }

    private int charAt(int cookie) {
        return name.charAt(getPosition(cookie));
    }

    public int getPreviousStart() {
        int cookie = initIteration();
        do {
            cookie = prevPos(cookie);
        } while (!isSegmentDelimiter(cookie));
        return getPosition(cookie) + 1;
    }

    public int getNextEnd() {
        int cookie = initIteration();
        do {
            cookie = nextPos(cookie);
        } while (!isSegmentDelimiter(cookie));
        return getPosition(cookie);
    }

    public boolean nextSegmentEquals(String other) {
        return nextSegmentEquals(other, 0, other.length());
    }

    public boolean nextSegmentEquals(String other, int offs, int len) {
        int cookie = initIteration();
        int strPos = 0;
        for (;;) {
            cookie = nextPos(cookie);
            if (isSegmentDelimiter(cookie)) {
                return strPos == len;
            }
            if (strPos == len) {
                return false;
            }
            if (other.charAt(offs + strPos) != charAt(cookie)) {
                return false;
            }
            strPos++;
        }
    }

    public String getNextSegment() {
        final StringBuilder b = new StringBuilder();
        int cookie = initIteration();
        for (;;) {
            cookie = nextPos(cookie);
            if (isSegmentDelimiter(cookie)) {
                return b.toString();
            }
            b.append((char) charAt(cookie));
        }
    }

    public boolean previousSegmentEquals(String other) {
        return previousSegmentEquals(other, 0, other.length());
    }

    public boolean previousSegmentEquals(final String other, final int offs, final int len) {
        int cookie = initIteration();
        int strPos = len;
        for (;;) {
            strPos--;
            cookie = prevPos(cookie);
            if (isSegmentDelimiter(cookie)) {
                return strPos == -1;
            }
            if (strPos == -1) {
                return false;
            }
            if (other.charAt(offs + strPos) != charAt(cookie)) {
                return false;
            }
        }
    }

    public String getPreviousSegment() {
        final StringBuilder b = new StringBuilder();
        int cookie = initIteration();
        for (;;) {
            cookie = prevPos(cookie);
            if (isSegmentDelimiter(cookie)) {
                return b.reverse().toString();
            }
            b.append((char) charAt(cookie));
        }
    }

    public boolean hasNext() {
        return pos < name.length();
    }

    public boolean hasPrevious() {
        return pos > -1;
    }

    public void next() {
        pos = getNextEnd();
    }

    public void previous() {
        pos = getPreviousStart() - 1;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        // generated code relies on this behavior
        return getName();
    }
}
