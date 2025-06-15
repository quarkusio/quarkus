package io.quarkus.qute.deployment;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.deployment.QuteProcessor.MatchResult;

final class TemplateExpressionMatchesBuildItem extends MultiBuildItem {

    final String templateGeneratedId;

    private final Map<Integer, MatchResult> generatedIdsToMatches;

    public TemplateExpressionMatchesBuildItem(String templateGeneratedId,
            Map<Integer, MatchResult> generatedIdsToMatches) {
        this.templateGeneratedId = templateGeneratedId;
        this.generatedIdsToMatches = generatedIdsToMatches;
    }

    MatchResult getMatch(Integer generatedId) {
        return generatedIdsToMatches.get(generatedId);
    }

    Map<Integer, MatchResult> getGeneratedIdsToMatches() {
        return generatedIdsToMatches;
    }

}
