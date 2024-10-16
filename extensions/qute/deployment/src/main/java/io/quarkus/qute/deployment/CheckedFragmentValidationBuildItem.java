package io.quarkus.qute.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.Expression;

final class CheckedFragmentValidationBuildItem extends MultiBuildItem {

    final String templateGeneratedId;
    final List<Expression> fragmentExpressions;
    final CheckedTemplateBuildItem checkedTemplate;

    public CheckedFragmentValidationBuildItem(String templateGeneratedId,
            List<Expression> fragmentExpressions,
            CheckedTemplateBuildItem checkedTemplate) {
        this.templateGeneratedId = templateGeneratedId;
        this.fragmentExpressions = fragmentExpressions;
        this.checkedTemplate = checkedTemplate;
    }

}
