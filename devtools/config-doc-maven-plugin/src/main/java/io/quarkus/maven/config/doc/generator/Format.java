package io.quarkus.maven.config.doc.generator;

import java.util.Set;

public enum Format {

    asciidoc("adoc", Set.of(Format.DEFAULT_THEME)),
    markdown("md", Set.of(Format.DEFAULT_THEME, "github"));

    public static final String DEFAULT_THEME = "default";

    private final String extension;
    private final Set<String> supportedThemes;

    private Format(String extension, Set<String> supportedThemes) {
        this.extension = extension;
        this.supportedThemes = supportedThemes;
    }

    public String getExtension() {
        return extension;
    }

    public String normalizeTheme(String theme) {
        theme = theme.trim();

        return supportedThemes.contains(theme) ? theme : DEFAULT_THEME;
    }

    public static Format normalizeFormat(String format) {
        format = format.trim();

        return Format.valueOf(format);
    }
}
