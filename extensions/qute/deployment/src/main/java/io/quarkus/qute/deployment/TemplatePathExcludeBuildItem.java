package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is used to exclude template files found in template roots. Excluded templates are
 * neither parsed nor validated during build and are not available at runtime.
 * <p>
 * The matched input is the file path relative from the root directory and the {@code /} is used as a path separator.
 */
public final class TemplatePathExcludeBuildItem extends MultiBuildItem {

    private final String regexPattern;

    public TemplatePathExcludeBuildItem(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

}
