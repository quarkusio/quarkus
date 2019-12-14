package io.quarkus.qute.api;

import io.quarkus.qute.Template;
import io.quarkus.qute.Variant;

/**
 * 
 * @see Variant
 */
public interface VariantTemplate extends Template {

    /**
     * Attribute key - all template {@link Variant}s found.
     */
    String VARIANTS = "variants";

    /**
     * Attribute key - a selected {@link Variant}.
     */
    String SELECTED_VARIANT = "selectedVariant";

}
