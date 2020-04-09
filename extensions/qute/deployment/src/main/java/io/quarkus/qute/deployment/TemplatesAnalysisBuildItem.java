package io.quarkus.qute.deployment;

import java.util.List;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qute.Expression;

/**
 * Represents the result of analysis of all templates.
 */
public final class TemplatesAnalysisBuildItem extends SimpleBuildItem {

    private final List<TemplateAnalysis> analysis;

    public TemplatesAnalysisBuildItem(List<TemplateAnalysis> analysis) {
        this.analysis = analysis;
    }

    public List<TemplateAnalysis> getAnalysis() {
        return analysis;
    }

    static class TemplateAnalysis {

        // Path or other user-defined id; may be null
        public final String id;
        public final String generatedId;
        public final Set<Expression> expressions;
        public final String path;

        public TemplateAnalysis(String id, String generatedId, Set<Expression> expressions, String path) {
            this.id = id;
            this.generatedId = generatedId;
            this.expressions = expressions;
            this.path = path;
        }

    }

}
