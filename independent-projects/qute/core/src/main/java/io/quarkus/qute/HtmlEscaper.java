package io.quarkus.qute;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.qute.TemplateNode.Origin;

public class HtmlEscaper implements ResultMapper {

    private final List<String> escapedContentTypes;

    public HtmlEscaper(List<String> escapedContentTypes) {
        this.escapedContentTypes = escapedContentTypes;
    }

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

    private boolean requiresDefaultEscaping(Variant variant) {
        String contentType = variant.getContentType();
        if (contentType == null) {
            return false;
        }
        for (String escaped : escapedContentTypes) {
            if (contentType.startsWith(escaped)) {
                return true;
            }
        }
        return false;
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
