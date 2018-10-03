/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.handlers;

import java.util.Arrays;

/**
 * This builder is not thread-safe.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
final class ByteStringBuilder {

    private static final int INVALID_US_ASCII_CODE_POINT = 0x3f;
    private static final int INVALID_UTF_8_CODE_POINT = 0xfffd;
    private byte[] content;
    private int length;

    public ByteStringBuilder(final int len) {
        this.content = new byte[len];
    }

    public ByteStringBuilder append(final boolean b) {
        appendLatin1(Boolean.toString(b));
        return this;
    }

    public ByteStringBuilder append(final char c) {
        return appendUtf8Raw((byte) c);
    }

    public static int getUtf8LengthOf(final int c) {
        if (c < 0x80) {
            return 1;
        } else if (c < 0x800) {
            return 2;
        } else if (c < 0x10000) {
            return 3;
        } else if (c < 0x110000) {
            return 4;
        }
        return 1;
    }

    public ByteStringBuilder appendUtf8Raw(final int codePoint) {
        if (codePoint < 0) {
            appendUtf8Raw(INVALID_UTF_8_CODE_POINT);
        } else if (codePoint < 0x80) {
            doAppend((byte) codePoint);
        } else if (codePoint < 0x800) {
            doAppend((byte) (0xC0 | 0x1F & codePoint >>> 6));
            doAppend((byte) (0x80 | 0x3F & codePoint));
        } else if (codePoint < 0x10000) {
            doAppend((byte) (0xE0 | 0x0F & codePoint >>> 12));
            doAppend((byte) (0x80 | 0x3F & codePoint >>> 6));
            doAppend((byte) (0x80 | 0x3F & codePoint));
        } else if (codePoint < 0x110000) {
            doAppend((byte) (0xF0 | 0x07 & codePoint >>> 18));
            doAppend((byte) (0x80 | 0x3F & codePoint >>> 12));
            doAppend((byte) (0x80 | 0x3F & codePoint >>> 6));
            doAppend((byte) (0x80 | 0x3F & codePoint));
        } else {
            appendUtf8Raw(INVALID_UTF_8_CODE_POINT);
        }
        return this;
    }

    public ByteStringBuilder append(final byte[] bytes) {
        int length = this.length;
        int bl = bytes.length;
        reserve(bl, false);
        System.arraycopy(bytes, 0, content, length, bl);
        this.length = length + bl;
        return this;
    }

    public ByteStringBuilder append(final byte[] bytes, final int offs, final int len) {
        reserve(len, false);
        int length = this.length;
        System.arraycopy(bytes, offs, content, length, len);
        this.length = length + len;
        return this;
    }

    public ByteStringBuilder appendUSASCII(final String s) {
        return appendUSASCII(s, 0, s.length());
    }

    public ByteStringBuilder appendUSASCII(final String s, final int maxLen) {
        return appendASCII(128, s, 0, s.length(), maxLen);
    }

    public ByteStringBuilder appendUSASCII(final String s, final int offs, final int len) {
        return appendASCII(128, s, offs, len, Integer.MAX_VALUE);
    }

    public ByteStringBuilder appendPrintUSASCII(final String s) {
        return appendPrintUSASCII(s, 0, s.length());
    }

    public ByteStringBuilder appendPrintUSASCII(final String s, final int maxLen) {
        return appendASCII(33, 126, s, 0, s.length(), maxLen);
    }

    public ByteStringBuilder appendPrintUSASCII(final String s, final int offs, final int len) {
        return appendASCII(33, 126, s, offs, len, Integer.MAX_VALUE);
    }

    public ByteStringBuilder appendLatin1(final String s) {
        return appendLatin1(s, 0, s.length());
    }

    public ByteStringBuilder appendLatin1(final String s, final int offs, final int len) {
        return appendASCII(256, s, offs, len, Integer.MAX_VALUE);
    }

    public ByteStringBuilder append(final String s) {
        return append(s, 0, s.length());
    }

    public ByteStringBuilder append(final String s, final int offs, final int len) {
        int c;
        int i = offs;
        while (i < len) {
            c = s.charAt(offs + i++);
            if (Character.isHighSurrogate((char) c)) {
                if (i < len) {
                    char t = s.charAt(offs + i++);
                    if (!Character.isLowSurrogate(t)) {
                        c = INVALID_UTF_8_CODE_POINT;
                    } else {
                        c = Character.toCodePoint((char) c, t);
                    }
                } else {
                    c = INVALID_UTF_8_CODE_POINT;
                }
            }
            appendUtf8Raw(c);
        }
        return this;
    }

    public int write(final String s, final int limit) {
        int result = 0;
        int c;
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            c = s.charAt(i);
            if (Character.isHighSurrogate((char) c)) {
                if (i < len) {
                    char t = s.charAt(++i);
                    if (!Character.isLowSurrogate(t)) {
                        c = INVALID_UTF_8_CODE_POINT;
                    } else {
                        c = Character.toCodePoint((char) c, t);
                    }
                } else {
                    c = INVALID_UTF_8_CODE_POINT;
                }
            }
            final int byteLen = getUtf8LengthOf(c);
            if (length + byteLen > limit) {
                break;
            }
            result = i;
            appendUtf8Raw(c);
        }
        return result;
    }

    public ByteStringBuilder append(final int i) {
        appendLatin1(Integer.toString(i));
        return this;
    }

    public ByteStringBuilder append(final long l) {
        appendLatin1(Long.toString(l));
        return this;
    }

    public ByteStringBuilder append(final ByteStringBuilder other) {
        append(other.content, 0, other.length);
        return this;
    }

    public byte[] toArray() {
        return Arrays.copyOf(content, length);
    }

    public byte byteAt(final int index) {
        if (index < 0 || index > length) throw new IndexOutOfBoundsException();
        return content[index];
    }

    public int capacity() {
        return content.length;
    }

    public int length() {
        return length;
    }

    public void setLength(final int newLength) {
        if (newLength > length) {
            // grow
            reserve(newLength - length, true);
        }
        length = newLength;
    }

    public boolean contentEquals(final byte[] other) {
        return contentEquals(other, 0, other.length);
    }

    public boolean contentEquals(final byte[] other, final int offs, final int length) {
        if (length != this.length) return false;
        for (int i = 0; i < length; i++) {
            if (content[i] != other[offs + i]) {
                return false;
            }
        }
        return true;
    }

    private ByteStringBuilder appendASCII(final int asciiLen, final String s, final int offs, final int len, final int maxLen) {
        return appendASCII(0, asciiLen, s, offs, len, maxLen);
    }

    private ByteStringBuilder appendASCII(final int minChar, final int maxChar, final String s, final int offs, final int len, final int maxLen) {
        reserve(len, false);
        char c;
        for (int i = 0; i < len; i++) {
            if (i >= maxLen) {
                break;
            }
            c = s.charAt(i + offs);
            if (c < minChar || c > maxChar) {
                doAppendNoCheck((byte) INVALID_US_ASCII_CODE_POINT);
            } else {
                doAppendNoCheck((byte) c);
            }
        }
        return this;
    }

    private void reserve(final int count, final boolean clear) {
        final int length = this.length;
        final byte[] content = this.content;
        int cl = content.length;
        if (cl - length >= count) {
            if (clear) Arrays.fill(content, length, length + count, (byte) 0);
            return;
        }
        // clear remainder
        if (clear) Arrays.fill(content, length, cl, (byte) 0);
        do {
            // not enough space... grow by 1.5x
            cl = cl + (cl + 1 >> 1);
            if (cl < 0) throw new IllegalStateException("Too large");
        } while (cl - length < count);
        this.content = Arrays.copyOf(content, cl);
    }

    private void doAppend(final byte b) {
        byte[] content = this.content;
        final int cl = content.length;
        final int length = this.length;
        if (length == cl) {
            content = this.content = Arrays.copyOf(content, cl + (cl + 1 >> 1));
        }
        content[length] = b;
        this.length = length + 1;
    }

    private void doAppendNoCheck(final byte b) {
        content[length++] = b;
    }
}