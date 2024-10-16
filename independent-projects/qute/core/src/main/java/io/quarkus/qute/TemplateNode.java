package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Tree node of a parsed template.
 *
 * @see Template#getNodes()
 * @see Template#findNodes(java.util.function.Predicate)
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
     * Returns the parameter declarations defined in this template node.
     *
     * @return a list of param declarations
     */
    default List<ParameterDeclaration> getParameterDeclarations() {
        return Collections.emptyList();
    }

    /**
     *
     * @return the origin of the node
     */
    Origin getOrigin();

    /**
     * Constant means a static text or a literal output expression.
     *
     * @return {@code true} if the node represents a constant
     * @see TextNode
     * @see Expression#isLiteral()
     */
    default boolean isConstant() {
        return false;
    }

    /**
     *
     * @return {@code true} if the node represents a section
     * @see SectionNode
     */
    default boolean isSection() {
        return kind() == Kind.SECTION;
    }

    /**
     *
     * @return {@code true} if the node represents a text
     * @see TextNode
     */
    default boolean isText() {
        return kind() == Kind.TEXT;
    }

    /**
     *
     * @return{@code true} if the node represents an output expression
     * @see ExpressionNode
     */
    default boolean isExpression() {
        return kind() == Kind.EXPRESSION;
    }

    /**
     * Returns the kind of this node.
     * <p>
     * Note that comments and line separators are never preserved in the parsed template tree.
     *
     * @return the kind
     */
    Kind kind();

    default TextNode asText() {
        throw new IllegalStateException();
    }

    default SectionNode asSection() {
        throw new IllegalStateException();
    }

    default ExpressionNode asExpression() {
        throw new IllegalStateException();
    }

    default ParameterDeclarationNode asParamDeclaration() {
        throw new IllegalStateException();
    }

    public enum Kind {
        /**
         * @see TextNode
         */
        TEXT,
        /**
         * @see SectionNode
         */
        SECTION,
        /**
         * @see ExpressionNode
         */
        EXPRESSION,
        /**
         * @see ParameterDeclarationNode
         */
        PARAM_DECLARATION,
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

        default boolean hasNonGeneratedTemplateId() {
            return !getTemplateId().equals(getTemplateGeneratedId());
        }

        /**
         *
         * @return the template variant
         */
        Optional<Variant> getVariant();

        default void appendTo(StringBuilder builder) {
            // It only makes sense to append the info for a template with an explicit id
            if (hasNonGeneratedTemplateId()) {
                builder.append(" template [").append(getTemplateId()).append("] ").append("line ").append(getLine());
            }
        }

        /**
         * @return {@code true} if the template node was not part of the original template, {@code false} otherwise
         */
        default boolean isSynthetic() {
            return getLine() == -1;
        }

    }

}
