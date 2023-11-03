package io.quarkus.qute;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Content type, locale and encoding.
 */
public final class Variant {

    public static Variant forContentType(String contentType) {
        return new Variant(Locale.getDefault(), StandardCharsets.UTF_8, contentType);
    }

    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String TEXT_XML = "text/xml";
    public final static String APPLICATION_JSON = "application/json";

    private final Locale locale;
    private final String contentType;
    private final Charset encoding;
    private final int hashCode;

    public Variant(Locale locale, Charset encoding, String contentType) {
        this.locale = locale;
        this.contentType = contentType;
        this.encoding = encoding;
        this.hashCode = Objects.hash(encoding, locale, contentType);
    }

    public Variant(Locale locale, String contentType, String encoding) {
        this(locale, encoding != null ? Charset.forName(encoding) : null, contentType);
    }

    public Locale getLocale() {
        return locale;
    }

    public String getMediaType() {
        return getContentType();
    }

    public String getContentType() {
        return contentType;
    }

    public String getEncoding() {
        return encoding != null ? encoding.name() : null;
    }

    public Charset getCharset() {
        return encoding;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Variant other = (Variant) obj;
        return Objects.equals(encoding, other.encoding) && Objects.equals(locale, other.locale)
                && Objects.equals(contentType, other.contentType);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Variant [locale=").append(locale).append(", contentType=").append(contentType).append(", encoding=")
                .append(encoding).append("]");
        return builder.toString();
    }

}
