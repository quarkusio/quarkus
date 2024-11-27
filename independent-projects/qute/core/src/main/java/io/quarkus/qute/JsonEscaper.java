package io.quarkus.qute;

import java.util.Optional;

import io.quarkus.qute.TemplateNode.Origin;

public class JsonEscaper extends CharReplacementResultMapper {

    @Override
    public boolean appliesTo(Origin origin, Object result) {
        if (result instanceof RawString) {
            return false;
        }
        Optional<Variant> variant = origin.getVariant();
        if (variant.isPresent()) {
            String contentType = variant.get().getContentType();
            if (contentType != null) {
                return contentType.startsWith(Variant.APPLICATION_JSON);
            }
        }
        return false;
    }

    protected String replacementFor(char c) {
        // All Unicode characters may be placed within the quotation marks,
        // except for the characters that MUST be escaped: quotation mark,
        // reverse solidus, and the control characters (U+0000 through U+001F).
        // See also https://datatracker.ietf.org/doc/html/rfc8259#autoid-10
        switch (c) {
            case '"':
                return "\\\"";
            case '\\':
                return "\\\\";
            case '\r':
                return "\\r";
            case '\b':
                return "\\b";
            case '\n':
                return "\\n";
            case '\t':
                return "\\t";
            case '\f':
                return "\\f";
            case '/':
                return "\\/";
            default:
                return c < 32 ? String.format("\\u%04x", (int) c) : null;
        }
    }
}