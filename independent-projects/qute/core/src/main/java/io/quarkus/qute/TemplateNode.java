package io.quarkus.qute;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Node of a template tree.
 */
public interface TemplateNode {

    /**
     * 
     * @param context
     * @return the result node
     */
    CompletionStage<ResultNode> resolve(ResolutionContext context);

    /**
     * 
     * @return a set of expressions
     */
    default Set<Expression> getExpressions() {
        return Collections.emptySet();
    }

    Origin getOrigin();

    interface Origin {

        int getLine();

        String getTemplateId();

    }

}
