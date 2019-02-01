package org.jboss.shamrock.runtime.configuration;

import java.util.NoSuchElementException;

import org.wildfly.common.Assert;

/**
 */
public final class NameIterator {
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
        Assert.checkMinimumParameter("pos", -1, pos);
        Assert.checkMaximumParameter("pos", name.length(), pos);
        if (pos != -1 && pos != name.length() && name.charAt(pos) != '.') throw new IllegalArgumentException("Position is not located at a delimiter");
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
     * Get the cursor position.  It will be {@code -1} if the cursor is at the beginning of the string, or {@code name.length()}
     * if it is at the end.
     *
     * @return the cursor position
     */
    public int getPosition() {
        return pos;
    }

    public int getPreviousStart() {
        final int pos = this.pos;
        if (pos == -1) {
            throw new NoSuchElementException();
        }
        final int idx = name.lastIndexOf('.', pos - 1);
        return idx + 1;
    }

    public int getNextEnd() {
        final int pos = this.pos;
        final int length = name.length();
        if (pos == length) {
            throw new NoSuchElementException();
        }
        final int idx = name.indexOf('.', pos + 1);
        return idx == -1 ? length : idx;
    }

    public boolean nextSegmentEquals(String other) {
        return nextSegmentEquals(other, 0, other.length());
    }

    public boolean nextSegmentEquals(String other, int offs, int len) {
        final int nextEnd = getNextEnd();
        final int pos = this.pos;
        return nextEnd - pos - 1 == len && name.regionMatches(pos + 1, other, offs, len);
    }

    public String getNextSegment() {
        final int nextEnd = getNextEnd();
        final int pos = this.pos;
        return name.substring(pos + 1, nextEnd);
    }

    public boolean previousSegmentEquals(String other) {
        return previousSegmentEquals(other, 0, other.length());
    }

    public boolean previousSegmentEquals(final String other, final int offs, final int len) {
        final int prevStart = getPreviousStart();
        final int pos = this.pos;
        return pos - prevStart == len && name.regionMatches(prevStart, other, offs, len);
    }

    public String getPreviousSegment() {
        final int prevStart = getPreviousStart();
        final int pos = this.pos;
        return name.substring(prevStart, pos);
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

    public String getHeadString() {
        return name.substring(0, pos);
    }

    public String getTailString() {
        return name.substring(pos + 1);
    }
}
