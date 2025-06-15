/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.resteasy.reactive.client.impl.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.internal.StringUtil;

/**
 * A copy of Netty's HttpPostBodyUtil which is not public Shared Static object between HttpMessageDecoder,
 * HttpPostRequestDecoder and HttpPostRequestEncoder
 */
final class QuarkusHttpPostBodyUtil {

    /**
     * Default Content-Type in binary form
     */
    public static final String DEFAULT_BINARY_CONTENT_TYPE = "application/octet-stream";

    /**
     * Default Content-Type in Text form
     */
    public static final String DEFAULT_TEXT_CONTENT_TYPE = "text/plain";

    /**
     * Allowed mechanism for multipart mechanism := "7bit" / "8bit" / "binary" Not allowed: "quoted-printable" /
     * "base64"
     */
    public enum TransferEncodingMechanism {
        /**
         * Default encoding
         */
        BIT7("7bit"),
        /**
         * Short lines but not in ASCII - no encoding
         */
        BIT8("8bit"),
        /**
         * Could be long text not in ASCII - no encoding
         */
        BINARY("binary");

        private final String value;

        TransferEncodingMechanism(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private QuarkusHttpPostBodyUtil() {
    }

    /**
     * This class intends to decrease the CPU in seeking ahead some bytes in HttpPostRequestDecoder
     */
    static class SeekAheadOptimize {
        byte[] bytes;
        int readerIndex;
        int pos;
        int origPos;
        int limit;
        ByteBuf buffer;

        /**
         * @param buffer
         *        buffer with a backing byte array
         */
        SeekAheadOptimize(ByteBuf buffer) {
            if (!buffer.hasArray()) {
                throw new IllegalArgumentException("buffer hasn't backing byte array");
            }
            this.buffer = buffer;
            bytes = buffer.array();
            readerIndex = buffer.readerIndex();
            origPos = pos = buffer.arrayOffset() + readerIndex;
            limit = buffer.arrayOffset() + buffer.writerIndex();
        }

        /**
         * @param minus
         *        this value will be used as (currentPos - minus) to set the current readerIndex in the buffer.
         */
        void setReadPosition(int minus) {
            pos -= minus;
            readerIndex = getReadPosition(pos);
            buffer.readerIndex(readerIndex);
        }

        /**
         * @param index
         *        raw index of the array (pos in general)
         *
         * @return the value equivalent of raw index to be used in readerIndex(value)
         */
        int getReadPosition(int index) {
            return index - origPos + readerIndex;
        }
    }

    /**
     * Find the first non whitespace
     *
     * @return the rank of the first non whitespace
     */
    static int findNonWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    /**
     * Find the end of String
     *
     * @return the rank of the end of string
     */
    static int findEndOfString(String sb) {
        int result;
        for (result = sb.length(); result > 0; result--) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }

    /**
     * Try to find first LF or CRLF as Line Breaking
     *
     * @param buffer
     *        the buffer to search in
     * @param index
     *        the index to start from in the buffer
     *
     * @return a relative position from index > 0 if LF or CRLF is found or < 0 if not found
     */
    static int findLineBreak(ByteBuf buffer, int index) {
        int toRead = buffer.readableBytes() - (index - buffer.readerIndex());
        int posFirstChar = buffer.bytesBefore(index, toRead, HttpConstants.LF);
        if (posFirstChar == -1) {
            // No LF, so neither CRLF
            return -1;
        }
        if (posFirstChar > 0 && buffer.getByte(index + posFirstChar - 1) == HttpConstants.CR) {
            posFirstChar--;
        }
        return posFirstChar;
    }

    /**
     * Try to find last LF or CRLF as Line Breaking
     *
     * @param buffer
     *        the buffer to search in
     * @param index
     *        the index to start from in the buffer
     *
     * @return a relative position from index > 0 if LF or CRLF is found or < 0 if not found
     */
    static int findLastLineBreak(ByteBuf buffer, int index) {
        int candidate = findLineBreak(buffer, index);
        int findCRLF = 0;
        if (candidate >= 0) {
            if (buffer.getByte(index + candidate) == HttpConstants.CR) {
                findCRLF = 2;
            } else {
                findCRLF = 1;
            }
            candidate += findCRLF;
        }
        int next;
        while (candidate > 0 && (next = findLineBreak(buffer, index + candidate)) >= 0) {
            candidate += next;
            if (buffer.getByte(index + candidate) == HttpConstants.CR) {
                findCRLF = 2;
            } else {
                findCRLF = 1;
            }
            candidate += findCRLF;
        }
        return candidate - findCRLF;
    }

    /**
     * Try to find the delimiter, with LF or CRLF in front of it (added as delimiters) if needed
     *
     * @param buffer
     *        the buffer to search in
     * @param index
     *        the index to start from in the buffer
     * @param delimiter
     *        the delimiter as byte array
     * @param precededByLineBreak
     *        true if it must be preceded by LF or CRLF, else false
     *
     * @return a relative position from index > 0 if delimiter found designing the start of it (including LF or CRLF is
     *         asked) or a number < 0 if delimiter is not found
     *
     * @throws IndexOutOfBoundsException
     *         if {@code offset + delimiter.length} is greater than {@code buffer.capacity}
     */
    static int findDelimiter(ByteBuf buffer, int index, byte[] delimiter, boolean precededByLineBreak) {
        final int delimiterLength = delimiter.length;
        final int readerIndex = buffer.readerIndex();
        final int writerIndex = buffer.writerIndex();
        int toRead = writerIndex - index;
        int newOffset = index;
        boolean delimiterNotFound = true;
        while (delimiterNotFound && delimiterLength <= toRead) {
            // Find first position: delimiter
            int posDelimiter = buffer.bytesBefore(newOffset, toRead, delimiter[0]);
            if (posDelimiter < 0) {
                return -1;
            }
            newOffset += posDelimiter;
            toRead -= posDelimiter;
            // Now check for delimiter
            if (toRead >= delimiterLength) {
                delimiterNotFound = false;
                for (int i = 0; i < delimiterLength; i++) {
                    if (buffer.getByte(newOffset + i) != delimiter[i]) {
                        newOffset++;
                        toRead--;
                        delimiterNotFound = true;
                        break;
                    }
                }
            }
            if (!delimiterNotFound) {
                // Delimiter found, find if necessary: LF or CRLF
                if (precededByLineBreak && newOffset > readerIndex) {
                    if (buffer.getByte(newOffset - 1) == HttpConstants.LF) {
                        newOffset--;
                        // Check if CR before: not mandatory to be there
                        if (newOffset > readerIndex && buffer.getByte(newOffset - 1) == HttpConstants.CR) {
                            newOffset--;
                        }
                    } else {
                        // Delimiter with Line Break could be further: iterate after first char of delimiter
                        newOffset++;
                        toRead--;
                        delimiterNotFound = true;
                        continue;
                    }
                }
                return newOffset - readerIndex;
            }
        }
        return -1;
    }

    /**
     * copied from {@link HttpPostRequestDecoder}
     *
     * @param contentType
     *
     * @return
     */
    public static String[] getMultipartDataBoundary(String contentType) {
        // Check if Post using "multipart/form-data; boundary=--89421926422648 [; charset=xxx]"
        String[] headerContentType = splitHeaderContentType(contentType);
        final String multiPartHeader = HttpHeaderValues.MULTIPART_FORM_DATA.toString();
        if (headerContentType[0].regionMatches(true, 0, multiPartHeader, 0, multiPartHeader.length())) {
            int mrank;
            int crank;
            final String boundaryHeader = HttpHeaderValues.BOUNDARY.toString();
            if (headerContentType[1].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
                mrank = 1;
                crank = 2;
            } else if (headerContentType[2].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
                mrank = 2;
                crank = 1;
            } else {
                return null;
            }
            String boundary = StringUtil.substringAfter(headerContentType[mrank], '=');
            if (boundary == null) {
                throw new HttpPostRequestDecoder.ErrorDataDecoderException("Needs a boundary value");
            }
            if (boundary.charAt(0) == '"') {
                String bound = boundary.trim();
                int index = bound.length() - 1;
                if (bound.charAt(index) == '"') {
                    boundary = bound.substring(1, index);
                }
            }
            final String charsetHeader = HttpHeaderValues.CHARSET.toString();
            if (headerContentType[crank].regionMatches(true, 0, charsetHeader, 0, charsetHeader.length())) {
                String charset = StringUtil.substringAfter(headerContentType[crank], '=');
                if (charset != null) {
                    return new String[] { "--" + boundary, charset };
                }
            }
            return new String[] { "--" + boundary };
        }
        return null;
    }

    private static String[] splitHeaderContentType(String sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;
        aStart = findNonWhitespace(sb, 0);
        aEnd = sb.indexOf(';');
        if (aEnd == -1) {
            return new String[] { sb, "", "" };
        }
        bStart = findNonWhitespace(sb, aEnd + 1);
        if (sb.charAt(aEnd - 1) == ' ') {
            aEnd--;
        }
        bEnd = sb.indexOf(';', bStart);
        if (bEnd == -1) {
            bEnd = findEndOfString(sb);
            return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), "" };
        }
        cStart = findNonWhitespace(sb, bEnd + 1);
        if (sb.charAt(bEnd - 1) == ' ') {
            bEnd--;
        }
        cEnd = findEndOfString(sb);
        return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), sb.substring(cStart, cEnd) };
    }
}
