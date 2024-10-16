package io.quarkus.qute;

import java.util.List;
import java.util.Optional;

import io.quarkus.qute.TemplateNode.Origin;

public class HtmlEscaper extends CharReplacementResultMapper {

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

    protected String replacementFor(char c) {
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
