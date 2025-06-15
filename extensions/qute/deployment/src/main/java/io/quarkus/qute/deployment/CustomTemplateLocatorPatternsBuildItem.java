package io.quarkus.qute.deployment;

import java.util.Collection;
import java.util.regex.Pattern;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * List of template locations in form of RegEx located by custom locators that must not be validated as custom locators
 * are not available at build time.
 */
public final class CustomTemplateLocatorPatternsBuildItem extends SimpleBuildItem {

    private final Collection<Pattern> locationPatterns;

    public CustomTemplateLocatorPatternsBuildItem(Collection<Pattern> locationPatterns) {
        this.locationPatterns = locationPatterns;
    }

    public Collection<Pattern> getLocationPatterns() {
        return locationPatterns;
    }
}
