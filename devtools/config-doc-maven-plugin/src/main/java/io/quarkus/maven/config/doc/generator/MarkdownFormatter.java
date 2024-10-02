package io.quarkus.maven.config.doc.generator;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;
import io.quarkus.maven.config.doc.GenerateConfigDocMojo.Context;

final class MarkdownFormatter extends AbstractFormatter {

    private static final String MORE_INFO_ABOUT_TYPE_FORMAT = "[🛈](#%s)";

    MarkdownFormatter(JavadocRepository javadocRepository) {
        super(javadocRepository, false);
    }

    @Override
    protected JavadocFormat javadocFormat() {
        return JavadocFormat.MARKDOWN;
    }

    @Override
    public String formatSectionTitle(ConfigSection configSection) {
        return "&nbsp;".repeat(configSection.getLevel() * 4) + super.formatSectionTitle(configSection);
    }

    @Override
    protected String moreInformationAboutType(Context context, String anchorRoot, String type) {
        return MORE_INFO_ABOUT_TYPE_FORMAT.formatted(anchorRoot);
    }

    @Override
    protected String link(String href, String description) {
        return String.format("[%2$s](%1$s)", href, description);
    }

    @Override
    protected String tooltip(String value, String javadocDescription) {
        // we don't have tooltip support in Markdown
        return "`" + value + "`";
    }
}
