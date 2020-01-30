package io.quarkus.qute;

import java.util.Collections;
import java.util.Optional;
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

    /**
     * 
     * @return the origin of the node
     */
    Origin getOrigin();

    /**
     * Represents an origin of a template node.
     */
    public interface Origin {

        int getLine();

        int getLineCharacter();

        String getTemplateId();

        String getTemplateGeneratedId();

        Optional<Variant> getVariant();

    }

}
