package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

/**
 * @author Stuart Douglas
 */
public class MultipartParser {

    /**
     * The Horizontal Tab ASCII character value;
     */
    public static final byte HTAB = 0x09;

    /**
     * The Carriage Return ASCII character value.
     */
    public static final byte CR = 0x0D;

    /**
     * The Line Feed ASCII character value.
     */
    public static final byte LF = 0x0A;

    /**
     * The Space ASCII character value;
     */
    public static final byte SP = 0x20;

    /**
     * The dash (-) ASCII character value.
     */
    public static final byte DASH = 0x2D;

    /**
     * A byte sequence that precedes a boundary (<code>CRLF--</code>).
     */
    private static final byte[] BOUNDARY_PREFIX = { CR, LF, DASH, DASH };
    public static final String CONTENT_TRANSFER_ENCODING = "content-transfer-encoding";

    public interface PartHandler {
        void beginPart(final CaseInsensitiveMap<String> headers);

        void data(final ByteBuffer buffer) throws IOException;

        void endPart();
    }

    public static ParseState beginParse(final PartHandler handler, final byte[] boundary, final String requestCharset) {

        // We prepend CR/LF to the boundary to chop trailing CR/LF from
        // body-data tokens.
        byte[] boundaryToken = new byte[boundary.length + BOUNDARY_PREFIX.length];
        System.arraycopy(BOUNDARY_PREFIX, 0, boundaryToken, 0, BOUNDARY_PREFIX.length);
        System.arraycopy(boundary, 0, boundaryToken, BOUNDARY_PREFIX.length, boundary.length);
        return new ParseState(handler, requestCharset, boundaryToken);
    }

    public static class ParseState {
        private final PartHandler partHandler;
        private String requestCharset;
        /**
         * The boundary, complete with the initial CRLF--
         */
        private final byte[] boundary;

        //0=preamble
        private int state = 0;
        private int subState = Integer.MAX_VALUE; // used for preamble parsing
        private ByteArrayOutputStream currentString = null;
        private String currentHeaderName = null;
        private CaseInsensitiveMap<String> headers;
        private Encoding encodingHandler;

        public ParseState(final PartHandler partHandler, String requestCharset, final byte[] boundary) {
            this.partHandler = partHandler;
            this.requestCharset = requestCharset;
            this.boundary = boundary;
        }

        public void setCharacterEncoding(String encoding) {
            requestCharset = encoding;
        }

        public void parse(ByteBuffer buffer) throws IOException {
            while (buffer.hasRemaining()) {
                switch (state) {
                    case 0: {
                        preamble(buffer);
                        break;
                    }
                    case 1: {
                        headerName(buffer);
                        break;
                    }
                    case 2: {
                        headerValue(buffer);
                        break;
                    }
                    case 3: {
                        entity(buffer);
                        break;
                    }
                    case -1: {
                        return;
                    }
                    default: {
                        throw new IllegalStateException("" + state);
                    }
                }
            }
        }

        private void preamble(final ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (subState >= 0) {
                    //handle the case of no preamble. In this case there is no CRLF
                    if (subState == Integer.MAX_VALUE) {
                        if (boundary[2] == b) {
                            subState = 2;
                        } else {
                            subState = 0;
                        }
                    }
                    if (b == boundary[subState]) {
                        subState++;
                        if (subState == boundary.length) {
                            subState = -1;
                        }
                    } else if (b == boundary[0]) {
                        subState = 1;
                    } else {
                        subState = 0;
                    }
                } else if (subState == -1) {
                    if (b == CR) {
                        subState = -2;
                    }
                } else if (subState == -2) {
                    if (b == LF) {
                        subState = 0;
                        state = 1;//preamble is done
                        headers = new CaseInsensitiveMap<String>();
                        return;
                    } else {
                        subState = -1;
                    }
                }
            }
        }

        private void headerName(final ByteBuffer buffer) throws MalformedMessageException, UnsupportedEncodingException {
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (b == ':') {
                    if (currentString == null || subState != 0) {
                        throw new MalformedMessageException();
                    } else {
                        currentHeaderName = new String(currentString.toByteArray(), requestCharset);
                        currentString.reset();
                        subState = 0;
                        state = 2;
                        return;
                    }
                } else if (b == CR) {
                    if (currentString != null) {
                        throw new MalformedMessageException();
                    } else {
                        subState = 1;
                    }
                } else if (b == LF) {
                    if (currentString != null || subState != 1) {
                        throw new MalformedMessageException();
                    }
                    state = 3;
                    subState = 0;
                    partHandler.beginPart(headers);
                    //select the appropriate encoding
                    String encoding = headers.getFirst(CONTENT_TRANSFER_ENCODING);
                    if (encoding == null) {
                        encodingHandler = new IdentityEncoding();
                    } else if (encoding.equalsIgnoreCase("base64")) {
                        encodingHandler = new Base64Encoding();
                    } else if (encoding.equalsIgnoreCase("quoted-printable")) {
                        encodingHandler = new QuotedPrintableEncoding();
                    } else {
                        encodingHandler = new IdentityEncoding();
                    }
                    headers = null;
                    return;

                } else {
                    if (subState != 0) {
                        throw new MalformedMessageException();
                    } else if (currentString == null) {
                        currentString = new ByteArrayOutputStream();
                    }
                    currentString.write(b);
                }
            }
        }

        private void headerValue(final ByteBuffer buffer) throws MalformedMessageException, UnsupportedEncodingException {
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (subState == 2) {
                    if (b == CR) { //end of headers section
                        headers.put(currentHeaderName.trim(),
                                Collections.singletonList(new String(currentString.toByteArray(), requestCharset).trim()));
                        //set state for headerName to verify end of headers section
                        state = 1;
                        subState = 1; //CR already encountered
                        currentString = null;
                        return;
                    } else if (b == SP || b == HTAB) { //multi-line header
                        currentString.write(b);
                        subState = 0;
                    } else { //next header name
                        headers.put(currentHeaderName.trim(),
                                Collections.singletonList(new String(currentString.toByteArray(), requestCharset).trim()));
                        //set state for headerName to collect next header's name
                        state = 1;
                        subState = 0;
                        //start name collection for headerName to finish
                        currentString = new ByteArrayOutputStream();
                        currentString.write(b);
                        return;
                    }
                } else if (b == CR) {
                    subState = 1;
                } else if (b == LF) {
                    if (subState != 1) {
                        throw new MalformedMessageException();
                    }
                    subState = 2;
                } else {
                    if (subState != 0) {
                        throw new MalformedMessageException();
                    }
                    currentString.write(b);
                }
            }
        }

        private void entity(final ByteBuffer buffer) throws IOException {
            int startingSubState = subState;
            int pos = buffer.position();
            while (buffer.hasRemaining()) {
                final byte b = buffer.get();
                if (subState >= 0) {
                    if (b == boundary[subState]) {
                        //if we have a potential boundary match
                        subState++;
                        if (subState == boundary.length) {
                            startingSubState = 0;
                            //we have our data
                            ByteBuffer retBuffer = buffer.duplicate();
                            retBuffer.position(pos);

                            retBuffer.limit(Math.max(buffer.position() - boundary.length, 0));
                            encodingHandler.handle(partHandler, retBuffer);
                            partHandler.endPart();
                            subState = -1;
                        }
                    } else if (b == boundary[0]) {
                        //we started half way through a boundary, but it turns out we did not actually meet the boundary condition
                        //so we call the part handler with our copy of the boundary data
                        if (startingSubState > 0) {
                            encodingHandler.handle(partHandler, ByteBuffer.wrap(boundary, 0, startingSubState));
                            startingSubState = 0;
                        }
                        subState = 1;
                    } else {
                        //we started half way through a boundary, but it turns out we did not actually meet the boundary condition
                        //so we call the part handler with our copy of the boundary data
                        if (startingSubState > 0) {
                            encodingHandler.handle(partHandler, ByteBuffer.wrap(boundary, 0, startingSubState));
                            startingSubState = 0;
                        }
                        subState = 0;
                    }
                } else if (subState == -1) {
                    if (b == CR) {
                        subState = -2;
                    } else if (b == DASH) {
                        subState = -3;
                    }
                } else if (subState == -2) {
                    if (b == LF) {
                        //ok, we have our data
                        subState = 0;
                        state = 1;
                        headers = new CaseInsensitiveMap<String>();
                        return;
                    } else if (b == DASH) {
                        subState = -3;
                    } else {
                        subState = -1;
                    }
                } else if (subState == -3) {
                    if (b == DASH) {
                        state = -1; //we are done
                        return;
                    } else {
                        subState = -1;
                    }
                }
            }
            //handle the data we read so far
            ByteBuffer retBuffer = buffer.duplicate();
            retBuffer.position(pos);
            if (subState == 0) {
                //if we end partially through a boundary we do not handle the data
                encodingHandler.handle(partHandler, retBuffer);
            } else if (retBuffer.remaining() > subState && subState > 0) {
                //we have some data to handle, and the end of the buffer might be a boundary match
                retBuffer.limit(retBuffer.limit() - subState);
                encodingHandler.handle(partHandler, retBuffer);
            }
        }

        public boolean isComplete() {
            return state == -1;
        }
    }

    private interface Encoding {
        void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException;
    }

    private static class IdentityEncoding implements Encoding {

        @Override
        public void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException {
            handler.data(rawData);
            rawData.clear();
        }
    }

    private static class Base64Encoding implements Encoding {

        private final Base64.Decoder decoder = Base64.getMimeDecoder();

        @Override
        public void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException {
            try {
                ByteBuffer buf = decoder.decode(rawData);
                handler.data(buf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class QuotedPrintableEncoding implements Encoding {

        boolean equalsSeen;
        byte firstCharacter;

        @Override
        public void handle(final PartHandler handler, final ByteBuffer rawData) throws IOException {
            boolean equalsSeen = this.equalsSeen;
            byte firstCharacter = this.firstCharacter;
            ByteBuffer buf = ByteBuffer.allocate(1024);
            try {
                while (rawData.hasRemaining()) {
                    byte b = rawData.get();
                    if (equalsSeen) {
                        if (firstCharacter == 0) {
                            if (b == '\n' || b == '\r') {
                                //soft line break
                                //ignore
                                equalsSeen = false;
                            } else {
                                firstCharacter = b;
                            }
                        } else {
                            int result = Character.digit((char) firstCharacter, 16);
                            result <<= 4; //shift it 4 bytes and then add the next value to the end
                            result += Character.digit((char) b, 16);
                            buf.put((byte) result);
                            equalsSeen = false;
                            firstCharacter = 0;
                        }
                    } else if (b == '=') {
                        equalsSeen = true;
                    } else {
                        buf.put(b);
                        if (!buf.hasRemaining()) {
                            buf.flip();
                            handler.data(buf);
                            buf.clear();
                        }
                    }
                }
                buf.flip();
                handler.data(buf);
            } finally {
                this.equalsSeen = equalsSeen;
                this.firstCharacter = firstCharacter;
            }
        }
    }

    public static class MalformedMessageException extends RuntimeException {
    }

}
