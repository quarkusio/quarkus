package io.quarkus.annotation.processor.generate_doc;

import java.util.List;

import io.quarkus.annotation.processor.Constants;

interface DocFormatter {
    default String getAnchor(ConfigDocKey item) {
        return item.getKey().replaceAll("[<\">]", Constants.EMPTY);
    }

    String format(List<ConfigDocItem> configDocItems);

    String format(ConfigDocKey configDocKey);

    String format(ConfigDocSection configDocSection);
}
