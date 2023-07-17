package io.quarkus.qute.deployment;

import java.util.List;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.Expression;

final class CheckedFragmentValidationBuildItem extends MultiBuildItem {

    final String templateGeneratedId;
    final String templateId;
    final String fragmentId;
    final List<Expression> fragmentExpressions;
    final MethodInfo method;

    public CheckedFragmentValidationBuildItem(String templateGeneratedId, String templateId, String fragmentId,
            List<Expression> fragmentExpressions,
            MethodInfo method) {
        this.templateGeneratedId = templateGeneratedId;
        this.templateId = templateId;
        this.fragmentId = fragmentId;
        this.fragmentExpressions = fragmentExpressions;
        this.method = method;
    }

}
