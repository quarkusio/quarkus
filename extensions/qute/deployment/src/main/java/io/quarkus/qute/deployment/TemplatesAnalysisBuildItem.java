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

        public final String id;
        public final Set<Expression> expressions;
        public final TemplatePathBuildItem path;

        public TemplateAnalysis(String id, Set<Expression> expressions, TemplatePathBuildItem path) {
            this.id = id;
            this.expressions = expressions;
            this.path = path;
        }

    }

}
