package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
     * @return a list of expressions
     */
    default List<Expression> getExpressions() {
        return Collections.emptyList();
    }

    /**
     * 
     * @return the origin of the node
     */
    Origin getOrigin();

    /**
     * 
     * @return {@code true} if the node represents a constant
     */
    default boolean isConstant() {
        return false;
    }

    /**
     * Represents an origin of a template node.
     */
    public interface Origin {

        /**
         * 
         * @return the line where the node can be found
         */
        int getLine();

        /**
         * Note that this information is not available for all nodes.
         * <p>
         * However, it's always available for an expression node.
         * 
         * @return the line character the node starts
         */
        int getLineCharacterStart();

        /**
         * Note that this information is not available for all nodes.
         * <p>
         * However, it's always available for an expression node.
         * 
         * @return the line character the node ends
         */
        int getLineCharacterEnd();

        String getTemplateId();

        String getTemplateGeneratedId();

        /**
         * 
         * @return the template variant
         */
        Optional<Variant> getVariant();

    }

}
