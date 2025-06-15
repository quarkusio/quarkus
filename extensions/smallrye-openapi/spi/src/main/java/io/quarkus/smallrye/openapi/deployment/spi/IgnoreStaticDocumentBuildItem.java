package io.quarkus.smallrye.openapi.deployment.spi;

import java.util.regex.Pattern;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Ignore a static OpenAPI document included in extension and/or dependencies. Supports regular expressions.
 */
public final class IgnoreStaticDocumentBuildItem extends MultiBuildItem {

    private Pattern urlIgnorePattern = null;

    /**
     * @param urlIgnorePattern
     *        pattern to ignore when scanning static documents
     */
    public IgnoreStaticDocumentBuildItem(String urlIgnorePattern) {
        this.urlIgnorePattern = Pattern.compile(urlIgnorePattern);
    }

    public Pattern getUrlIgnorePattern() {
        return this.urlIgnorePattern;
    }
}
