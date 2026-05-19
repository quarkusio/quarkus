package io.quarkus.qute;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import io.quarkus.qute.TemplateNode.Origin;

/**
 * Represents a value expression. It could be a literal such as {@code 'foo'}. It could have a namespace such as {@code data}
 * for {@code data:name}. It could have several parts such as {@code item} and {@code name} for {@code item.name}.
 * <p>
 * A literal expression may also have additional parts that chain on the literal value, such as property accessors and virtual
 * methods. For example, {@code 'foo'.length} or {@code 1l.intValue}.
 *
 * @see Evaluator
 */
public interface Expression {

    /**
     *
     * @return the namespace, may be {@code null}
     * @see NamespaceResolver
     */
    String getNamespace();

    default boolean hasNamespace() {
        return getNamespace() != null;
    }

    /**
     * For a non-literal expression, each part represents either a property accessor or a virtual method invocation.
     * A literal expression always has at least one part with type info. It may also have additional parts that represent
     * property accessors or virtual methods chained on the literal value.
     *
     * @return the list of parts, is never {@code null}
     */
    List<Part> getParts();

    /**
     * A simple literal expression has exactly one part with type info and no chaining parts.
     * An expression with a literal base and additional chaining parts (e.g. {@code 'foo'.length}) is not considered a literal.
     *
     * @return true if it represents a simple literal without chaining parts
     * @see #getParts()
     * @see #getLiteralValue()
     */
    boolean isLiteral();

    /**
     * The literal value may be non-null even if {@link #isLiteral()} returns false, for expressions with a literal base
     * and additional chaining parts.
     *
     * @return the literal value, or null
     */
    CompletableFuture<Object> getLiteralValue();

    /**
     * The literal value may be non-null even if {@link #isLiteral()} returns false, for expressions with a literal base
     * and additional chaining parts.
     *
     * @return the literal value, or null
     */
    Object getLiteral();

    /**
     *
     * @return the literal value
     * @throws IllegalStateException If the expression does not represent a literal
     * @see Expression#isLiteral()
     */
    CompletionStage<Object> asLiteral();

    /**
     *
     * @return the origin
     */
    Origin getOrigin();

    /**
     *
     * @return the original value as defined in the template
     */
    String toOriginalString();

    default String collectTypeInfo() {
        if (!hasTypeInfo()) {
            return null;
        }
        return getParts().stream().map(Part::getTypeInfo).collect(Collectors.joining("."));
    }

    default boolean hasTypeInfo() {
        return getParts().get(0).getTypeInfo() != null;
    }

    /**
     * The id must be unique for the template.
     *
     * @return the generated id or {@code -1} for an expression that was not created by a parser
     */
    int getGeneratedId();

    /**
     *
     * @see Expression#getParts()
     */
    interface Part {

        /**
         *
         * @return the name of a property or virtual method
         */
        String getName();

        /**
         * An expression part may have a "type check information" attached. The string can be one of the following:
         * <ul>
         * <li>type info that represents a fully qualified type name (including type parameters) -
         * {@code |TYPE_INFO|<section-hint>};
         * for example {@code |org.acme.Foo|},
         * {@code |java.util.List<org.acme.Label>|} and {@code |org.acme.Foo|<when#123>}</li>
         * <li>property; for example {@code foo} and {@code foo<loop#123>}</li>
         * <li>virtual method; for example {@code foo.call(bar)} and {@code foo.getNames(10)<loop-element>}</li>
         * </ul>
         *
         * @return the type check info
         */
        String getTypeInfo();

        default boolean isVirtualMethod() {
            return false;
        }

        default VirtualMethodPart asVirtualMethod() {
            throw new IllegalStateException("Not a virtual method: " + toString() + " [typeInfo: " + getTypeInfo() + "]");
        }

    }

    /**
     * Part that represents a virtual method.
     */
    interface VirtualMethodPart extends Part {

        List<Expression> getParameters();

    }

}
