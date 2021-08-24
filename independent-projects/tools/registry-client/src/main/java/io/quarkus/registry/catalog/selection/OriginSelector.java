package io.quarkus.registry.catalog.selection;

import java.util.ArrayList;
import java.util.List;

public class OriginSelector {

    private final List<ExtensionOrigins> extOrigins;
    private final List<OriginCombination> completeCombinations = new ArrayList<>();

    private int highestRegistryPreference;

    public OriginSelector(List<ExtensionOrigins> extOrigins) {
        this.extOrigins = extOrigins;
    }

    public void calculateCompatibleCombinations() {
        if (extOrigins.isEmpty()) {
            return;
        }
        select(0, new OriginCombination());

        // log all the complete combincations
        //System.out.println("OriginSelector.calculateCompatibleCombinations of " + extOrigins.size() + " extensions, complete combinations: " + completeCombinations.size());
        //if (completeCombinations.isEmpty()) {
        //    System.out.println("  none");
        //} else {
        //    for (int i = 0; i < completeCombinations.size(); ++i) {
        //        final OriginCombination s = completeCombinations.get(i);
        //        System.out.println("Combination #" + (i + 1) + " score=" + calculateScore(s));
        //        s.getUniqueSortedOrigins()
        //                .forEach(o -> System.out.println(
        //                        " - " + o.getCatalog().getBom() + " " + o.getCatalog().isPlatform() + " " + o.getPreference()));
        //    }
        //}
    }

    public OriginCombination getRecommendedCombination() {
        if (completeCombinations.isEmpty()) {
            return null;
        }
        if (completeCombinations.size() == 1) {
            return completeCombinations.get(0);
        }
        // here we are going to be looking for the combination that include the most extensions
        // in the most preferred registry with the lowest total number of platforms BOMs to be imported
        double highestScore = 0;
        OriginCombination recommended = null;
        for (OriginCombination combination : completeCombinations) {
            final double score = calculateScore(combination);
            if (score > highestScore) {
                highestScore = score;
                recommended = combination;
            }
        }
        return recommended;
    }

    private double calculateScore(OriginCombination s) {
        double combinationScore = 0;
        for (OriginWithPreference o : s.getCollectedOrigins()) {
            combinationScore += Math.pow(extOrigins.size(),
                    highestRegistryPreference + 1 - o.getPreference().registryPreference)
                    * ((((double) Integer.MAX_VALUE) + 1 - o.getPreference().platformPreference) / Integer.MAX_VALUE);
        }
        return combinationScore;
    }

    private void select(int extIndex, OriginCombination combination) {
        if (extIndex >= extOrigins.size()) {
            throw new IllegalArgumentException(
                    "Extension index " + extIndex + " exceeded the total number of extensions " + extOrigins.size());
        }
        final ExtensionOrigins eo = extOrigins.get(extIndex);
        for (OriginWithPreference o : eo.getOrigins()) {
            highestRegistryPreference = Math.max(highestRegistryPreference, o.getPreference().registryPreference);
            final OriginCombination augmentedCombination = combination.add(eo.getExtensionKey(), o);
            if (augmentedCombination == null) {
                continue;
            }
            if (extOrigins.size() == augmentedCombination.size()) {
                completeCombinations.add(augmentedCombination);
                // we are collecting all possible combinations for now
                continue;
            }
            if (extIndex + 1 == extOrigins.size()) {
                return;
            }
            select(extIndex + 1, augmentedCombination);
        }
    }
}
