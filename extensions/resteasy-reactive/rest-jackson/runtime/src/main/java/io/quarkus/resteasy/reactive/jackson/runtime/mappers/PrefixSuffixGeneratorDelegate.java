package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

class PrefixSuffixGeneratorDelegate extends JsonGeneratorDelegate {

    private final String prefix;
    private final String suffix;

    PrefixSuffixGeneratorDelegate(JsonGenerator delegate, String prefix, String suffix) {
        super(delegate, false);
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        delegate.writeFieldName(prefix + name + suffix);
    }

    @Override
    public void writeFieldName(SerializableString name) throws IOException {
        delegate.writeFieldName(prefix + name.getValue() + suffix);
    }
}
