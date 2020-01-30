package io.quarkus.qute;

import java.util.Locale;
import java.util.Objects;

/**
 * Media type, locale and encoding.
 */
public final class Variant {

    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String TEXT_XML = "text/xml";
    public final static String APPLICATION_JSON = "application/json";

    public final Locale locale;
    public final String mediaType;
    public final String encoding;

    public Variant(Locale locale, String mediaType, String encoding) {
        this.locale = locale;
        this.mediaType = mediaType;
        this.encoding = encoding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(encoding, locale, mediaType);
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
                && Objects.equals(mediaType, other.mediaType);
    }

}
