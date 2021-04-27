package io.quarkus.qute.deployment;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.deployment.QuteProcessor.Match;

final class TemplateExpressionMatchesBuildItem extends MultiBuildItem {

    final String templateGeneratedId;

    private final Map<Integer, Match> generatedIdsToMatches;

    public TemplateExpressionMatchesBuildItem(String templateGeneratedId, Map<Integer, Match> generatedIdsToMatches) {
        this.templateGeneratedId = templateGeneratedId;
        this.generatedIdsToMatches = generatedIdsToMatches;
    }

    Match getMatch(Integer generatedId) {
        return generatedIdsToMatches.get(generatedId);
    }

    Map<Integer, Match> getGeneratedIdsToMatches() {
        return generatedIdsToMatches;
    }

}
