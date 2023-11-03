package io.quarkus.registry.catalog.selection;

import java.util.List;

public interface OriginSelector {

    static OriginSelector of(List<ExtensionOrigins> extOrigins) {
        //return new AllCombinationsOriginSelector(extOrigins);
        return new DefaultOriginSelector(extOrigins);
    }

    OriginCombination calculateRecommendedCombination();
}
