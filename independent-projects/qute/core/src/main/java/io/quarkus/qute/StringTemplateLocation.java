package io.quarkus.qute;

import java.io.Reader;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.qute.Parser.StringReader;
import io.quarkus.qute.TemplateLocator.TemplateLocation;

public class StringTemplateLocation implements TemplateLocation {

    private final String content;
    private final Optional<Variant> variant;
    private final URI source;

    public StringTemplateLocation(String content) {
        this(content, Optional.empty());
    }

    public StringTemplateLocation(String content, Optional<Variant> variant) {
        this(content, variant, null);
    }

    public StringTemplateLocation(String content, Optional<Variant> variant, URI source) {
        this.content = Objects.requireNonNull(content);
        this.variant = Objects.requireNonNull(variant);
        this.source = source;
    }

    @Override
    public Reader read() {
        return new StringReader(content);
    }

    @Override
    public Optional<Variant> getVariant() {
        return variant;
    }

    @Override
    public Optional<URI> getSource() {
        if (source != null) {
            return Optional.of(source);
        }
        return Optional.empty();
    }

}
