package org.jboss.protean.arc.processor;

import java.util.Map;

import org.jboss.jandex.IndexView;

/**
 * Build-time extension point.
 *
 * @author Martin Kouba
 */
public interface BuildProcessor {

    static final int DEFAULT_PRIORITY = 1000;

    static int compare(BuildProcessor p1, BuildProcessor p2) {
        return Integer.compare(p2.getPriority(), p1.getPriority());
    }

    /**
     * Processors with higher priority are called first.
     *
     * @return the priority
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * Initialize self. Processors are initialized before a bean deployment is constructed.
     *
     * @param index
     * @param contextData
     * @return {@code true} if the extension should be put into service, @{code false} otherwise
     */
    default boolean initialize(BuildContext buildContext) {
        return true;
    }

    interface BuildContext {

        /**
         *
         * @return the original deployment index
         */
        IndexView getIndex();

        /**
         *
         * @return a mutable map accessible from all processors
         */
        Map<String, Object> getContextData();

    }

}
