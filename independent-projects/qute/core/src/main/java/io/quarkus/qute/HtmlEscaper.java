package io.quarkus.qute;

import io.quarkus.qute.TemplateNode.Origin;
import java.util.Objects;
import java.util.Optional;

public class HtmlEscaper implements ResultMapper {

    @Override
    public boolean appliesTo(Origin origin, Object result) {
        if (result instanceof RawString) {
            return false;
        }
        Optional<Variant> variant = origin.getVariant();
        if (variant.isPresent()) {
            return requiresDefaultEscaping(variant.get());
        }
        return false;
    }

    @Override
    public String map(Object result, Expression expression) {
        return escape(result.toString());
    }

    String escape(CharSequence value) {
        if (Objects.requireNonNull(value).length() == 0) {
            return value.toString();
        }
        for (int i = 0; i < value.length(); i++) {
            String replacement = replacementFor(value.charAt(i));
            if (replacement != null) {
                // In most cases we will not need to escape the value at all
                return doEscape(value, i, new StringBuilder(value.subSequence(0, i)).append(replacement));
            }
        }
        return value.toString();
    }

    static boolean requiresDefaultEscaping(Variant variant) {
        return variant.getContentType() != null
                ? (Variant.TEXT_HTML.equals(variant.getContentType()) || Variant.TEXT_XML.equals(variant.getContentType()))
                : false;
    }

    private String doEscape(CharSequence value, int index, StringBuilder builder) {
        int length = value.length();
        while (++index < length) {
            char c = value.charAt(index);
            String replacement = replacementFor(c);
            if (replacement != null) {
                builder.append(replacement);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private String replacementFor(char c) {
        switch (c) {
            case '"':
                return "&quot;";
            case '\'':
                return "&#39;";
            case '&':
                return "&amp;";
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            default:
                return null;
        }
    }

}
