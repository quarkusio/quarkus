package io.quarkus.qute;

import java.io.Reader;
import java.util.Optional;

/**
 * Locates template sources.
 * 
 * @see Engine#getTemplate(String)
 */
public interface TemplateLocator extends WithPriority {

    /**
     * 
     * @param id
     * @return the template location for the given id
     */
    Optional<TemplateLocation> locate(String id);

    interface TemplateLocation {

        /**
         * A {@link Reader} instance produced by a locator is immediately closed right after the template content is parsed.
         * 
         * @return the reader
         */
        Reader read();

        /**
         * 
         * @return the template variant
         */
        Optional<Variant> getVariant();

    }

}
