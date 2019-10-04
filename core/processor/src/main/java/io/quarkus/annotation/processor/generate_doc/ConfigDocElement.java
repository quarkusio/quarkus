package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;

public interface ConfigDocElement {
    void accept(Writer writer, DocFormatter docFormatter) throws IOException;

    ConfigPhase getConfigPhase();

    boolean isWithinAMap();

    /**
     *
     * Map config will be at the end of generated doc.
     * Order build time config first
     * Otherwise maintain source code order.
     */
    default int compare(ConfigDocElement item) {
        if (isWithinAMap()) {
            if (item.isWithinAMap()) {
                return 0;
            }
            return 1;
        } else if (item.isWithinAMap()) {
            return -1;
        }

        int phaseComparison = ConfigPhase.COMPARATOR.compare(getConfigPhase(), item.getConfigPhase());
        if (phaseComparison == 0) {
            return 0;
        }

        return phaseComparison;
    }
}
