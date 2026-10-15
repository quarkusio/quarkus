package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.util.JsonGeneratorDelegate;

class PrefixSuffixGeneratorDelegate extends JsonGeneratorDelegate {

    private final String prefix;
    private final String suffix;

    PrefixSuffixGeneratorDelegate(JsonGenerator delegate, String prefix, String suffix) {
        super(delegate, false);
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public JsonGenerator writeName(String name) throws JacksonException {
        delegate.writeName(prefix + name + suffix);
        return this;
    }

    @Override
    public JsonGenerator writeName(SerializableString name) throws JacksonException {
        delegate.writeName(prefix + name.getValue() + suffix);
        return this;
    }
}
