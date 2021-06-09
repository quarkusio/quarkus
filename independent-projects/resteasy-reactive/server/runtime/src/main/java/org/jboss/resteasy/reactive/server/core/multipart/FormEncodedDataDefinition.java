package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.util.URLUtils;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;

/**
 * Parser definition for form encoded data. This handler takes effect for any request that has a mime type
 * of application/x-www-form-urlencoded. The handler attaches a {@link FormDataParser} to the chain
 * that can parse the underlying form data asynchronously.
 *
 * @author Stuart Douglas
 */
public class FormEncodedDataDefinition implements FormParserFactory.ParserDefinition<FormEncodedDataDefinition> {

    private static final Logger log = Logger.getLogger(FormEncodedDataDefinition.class);

    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private String defaultEncoding = "ISO-8859-1";
    private boolean forceCreation = false; //if the parser should be created even if the correct headers are missing
    private int maxParams = 1000;
    private long maxAttributeSize = 2048;

    public FormEncodedDataDefinition() {
    }

    @Override
    public FormDataParser create(final ResteasyReactiveRequestContext exchange) {
        String mimeType = exchange.serverRequest().getRequestHeader(HttpHeaders.CONTENT_TYPE);
        if (forceCreation || (mimeType != null && mimeType.startsWith(APPLICATION_X_WWW_FORM_URLENCODED))) {

            String charset = defaultEncoding;
            String contentType = exchange.serverRequest().getRequestHeader(HttpHeaders.CONTENT_TYPE);
            if (contentType != null) {
                String cs = HeaderUtil.extractQuotedValueFromHeader(contentType, "charset");
                if (cs != null) {
                    charset = cs;
                }
            }
            log.tracef("Created form encoded parser for %s", exchange);
            return new FormEncodedDataParser(charset, exchange, maxParams, maxAttributeSize);
        }
        return null;
    }

    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    public boolean isForceCreation() {
        return forceCreation;
    }

    public int getMaxParams() {
        return maxParams;
    }

    public long getMaxAttributeSize() {
        return maxAttributeSize;
    }

    public FormEncodedDataDefinition setMaxAttributeSize(long maxAttributeSize) {
        this.maxAttributeSize = maxAttributeSize;
        return this;
    }

    public FormEncodedDataDefinition setMaxParams(int maxParams) {
        this.maxParams = maxParams;
        return this;
    }

    public FormEncodedDataDefinition setForceCreation(boolean forceCreation) {
        this.forceCreation = forceCreation;
        return this;
    }

    public FormEncodedDataDefinition setDefaultEncoding(final String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
        return this;
    }

    private static final class FormEncodedDataParser implements ServerHttpRequest.ReadCallback, FormDataParser {

        private final ResteasyReactiveRequestContext exchange;
        private final FormData data;
        private final StringBuilder builder = new StringBuilder();
        private final long maxAttributeSize;
        private String name = null;
        private String charset;

        //0= parsing name
        //1=parsing name, decode required
        //2=parsing value
        //3=parsing value, decode required
        //4=finished
        private int state = 0;

        private FormEncodedDataParser(final String charset, final ResteasyReactiveRequestContext exchange, int maxParams,
                long maxAttributeSize) {
            this.exchange = exchange;
            this.charset = charset;
            this.data = new FormData(maxParams);
            this.maxAttributeSize = maxAttributeSize;
        }

        private void doParse(final ByteBuffer buffer) throws IOException {
            while (buffer.hasRemaining()) {
                byte n = buffer.get();
                switch (state) {
                    case 0: {
                        if (n == '=') {
                            if (builder.length() > maxAttributeSize) {
                                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
                            }
                            name = builder.toString();
                            builder.setLength(0);
                            state = 2;
                        } else if (n == '&') {
                            if (builder.length() > maxAttributeSize) {
                                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
                            }
                            addPair(builder.toString(), "");
                            builder.setLength(0);
                            state = 0;
                        } else if (n == '%' || n == '+' || n < 0) {
                            state = 1;
                            builder.append((char) (n & 0xFF));
                        } else {
                            builder.append((char) n);
                        }
                        break;
                    }
                    case 1: {
                        if (n == '=') {
                            if (builder.length() > maxAttributeSize) {
                                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
                            }
                            name = decodeParameterName(builder.toString(), charset, true, new StringBuilder());
                            builder.setLength(0);
                            state = 2;
                        } else if (n == '&') {
                            addPair(decodeParameterName(builder.toString(), charset, true, new StringBuilder()), "");
                            builder.setLength(0);
                            state = 0;
                        } else {
                            builder.append((char) (n & 0xFF));
                        }
                        break;
                    }
                    case 2: {
                        if (n == '&') {
                            if (builder.length() > maxAttributeSize) {
                                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
                            }
                            addPair(name, builder.toString());
                            builder.setLength(0);
                            state = 0;
                        } else if (n == '%' || n == '+' || n < 0) {
                            state = 3;
                            builder.append((char) (n & 0xFF));
                        } else {
                            builder.append((char) n);
                        }
                        break;
                    }
                    case 3: {
                        if (n == '&') {
                            if (builder.length() > maxAttributeSize) {
                                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
                            }
                            addPair(name, decodeParameterValue(name, builder.toString(), charset, true, new StringBuilder()));
                            builder.setLength(0);
                            state = 0;
                        } else {
                            builder.append((char) (n & 0xFF));
                        }
                        break;
                    }
                }
            }
            if (builder.length() > maxAttributeSize) {
                throw new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE);
            }
        }

        private void addPair(String name, String value) {
            //if there was exception during decoding ignore the parameter [UNDERTOW-1554]
            if (name != null && value != null) {
                data.add(name, value);
            }
        }

        private String decodeParameterValue(String name, String value, String charset, boolean decodeSlash,
                StringBuilder stringBuilder) {
            return URLUtils.decode(value, Charset.forName(charset), decodeSlash, stringBuilder);
        }

        private String decodeParameterName(String name, String charset, boolean decodeSlash, StringBuilder stringBuilder) {
            return URLUtils.decode(name, Charset.forName(charset), decodeSlash, stringBuilder);
        }

        @Override
        public void parse() throws Exception {
            if (exchange.getFormData() != null) {
                return;
            }
            exchange.suspend();
            exchange.serverRequest().setReadListener(this);
            exchange.serverRequest().resumeRequestInput();
        }

        @Override
        public FormData parseBlocking() throws IOException {
            final FormData existing = exchange.getFormData();
            if (existing != null) {
                return existing;
            }

            try (InputStream input = exchange.getInputStream()) {
                int c;
                byte[] data = new byte[1024];
                while ((c = input.read(data)) > 0) {
                    ByteBuffer buf = ByteBuffer.wrap(data, 0, c);
                    doParse(buf);
                }
                inputDone();
                return this.data;
            }
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public void setCharacterEncoding(final String encoding) {
            this.charset = encoding;
        }

        @Override
        public void done() {
            inputDone();
            exchange.resume();
        }

        private void inputDone() {
            if (state == 2) {
                addPair(name, builder.toString());
            } else if (state == 3) {
                addPair(name, decodeParameterValue(name, builder.toString(), charset, true, new StringBuilder()));
            } else if (builder.length() > 0) {
                if (state == 1) {
                    addPair(decodeParameterName(builder.toString(), charset, true, new StringBuilder()), "");
                } else {
                    addPair(builder.toString(), "");
                }
            }
            state = 4;
            exchange.setFormData(data);
        }

        @Override
        public void data(ByteBuffer data) {
            try {
                doParse(data);
            } catch (Exception e) {
                exchange.resume(e);
            }
        }
    }

}
