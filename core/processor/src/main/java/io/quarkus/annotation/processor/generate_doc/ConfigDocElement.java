package io.quarkus.annotation.processor.generate_doc;

public interface ConfigDocElement {
    String accept(DocFormatter docFormatter);

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
