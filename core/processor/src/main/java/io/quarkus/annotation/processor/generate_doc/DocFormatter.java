package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.quarkus.annotation.processor.Constants;

interface DocFormatter {
    default String getAnchor(String key) {
        return key.replaceAll("[<\">]", Constants.EMPTY);
    }

    void format(Writer writer, List<ConfigDocItem> configDocItems) throws IOException;

    void format(Writer writer, ConfigDocKey configDocKey) throws IOException;

    void format(Writer writer, ConfigDocSection configDocSection) throws IOException;
}
