package io.quarkus.qute;

import java.util.Locale;
import java.util.Objects;

/**
 * Media type, locale and encoding.
 */
public final class Variant {

    public static Variant forContentType(String contentType) {
        return new Variant(null, contentType, null);
    }

    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String TEXT_XML = "text/xml";
    public final static String APPLICATION_JSON = "application/json";

    private final Locale locale;
    private final String contentType;
    private final String encoding;

    public Variant(Locale locale, String contentType, String encoding) {
        this.locale = locale;
        this.contentType = contentType;
        this.encoding = encoding;
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
        return encoding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(encoding, locale, contentType);
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
