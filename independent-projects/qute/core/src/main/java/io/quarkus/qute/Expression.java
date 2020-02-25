package io.quarkus.qute;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.TemplateNode.Origin;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Represents a value expression. It could be a literal such as {@code 'foo'}. It could have a namespace such as {@code data}
 * for {@code data:name}. It could have several parts such as {@code item} and {@code name} for {@code item.name}.
 * <p>
 * An expression may have a "type check information" attached. The string has a form of {@code part1[. part2[. ...]} where each
 * part is one of the following:
 * <ul>
 * <li>type info that represents a fully qualified type name (including type parameters) - {@code |TYPE_INFO|<section-hint>};
 * for example {@code |org.acme.Foo|},
 * {@code |java.util.List<org.acme.Label>|} and {@code |org.acme.Foo|<for-element>}</li>
 * <li>property; for example {@code foo} and {@code foo<for-element>}</li>
 * <li>virtual method; for example {@code foo.call(|org.acme.Bar|)}</li>
 * </ul>
 * The first part is either a type info or a namespace placeholder.
 * 
 * @see Evaluator
 */
public final class Expression {

    static final Expression EMPTY = new Expression(null, Collections.emptyList(), null, null, null);

    /**
     * 
     * @param value
     * @return a non-contextual expression
     */
    public static Expression from(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY;
        }
        return Parser.parseExpression(value, Collections.emptyMap(), null);
    }

    public static Expression literal(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY;
        }
        Object literal = LiteralSupport.getLiteral(value);
        if (literal == null) {
            throw new IllegalArgumentException("Not a literal value: " + value);
        }
        return new Expression(null, Collections.singletonList(value), literal, null, null);
    }

    public final String namespace;
    public final List<String> parts;
    public final CompletableFuture<Object> literal;
    public final String typeCheckInfo;
    public final Origin origin;

    Expression(String namespace, List<String> parts, Object literal, String typeCheckInfo, Origin origin) {
        this.namespace = namespace;
        this.parts = parts;
        this.literal = literal != Result.NOT_FOUND ? CompletableFuture.completedFuture(literal) : null;
        this.typeCheckInfo = typeCheckInfo;
        this.origin = origin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(literalValue(), namespace, parts, typeCheckInfo, origin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Expression other = (Expression) obj;
        return Objects.equals(literalValue(), other.literalValue()) && Objects.equals(namespace, other.namespace)
                && Objects.equals(parts, other.parts) && Objects.equals(typeCheckInfo, other.typeCheckInfo)
                && Objects.equals(origin, other.origin);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Expression [namespace=").append(namespace).append(", parts=").append(parts).append(", literal=")
                .append(literalValue()).append(", typeCheckInfo=")
                .append(typeCheckInfo).append("]");
        return builder.toString();
    }

    public String toOriginalString() {
        StringBuilder builder = new StringBuilder();
        if (namespace != null) {
            builder.append(namespace);
            builder.append(":");
        }
        for (Iterator<String> iterator = parts.iterator(); iterator.hasNext();) {
            builder.append(iterator.next());
            if (iterator.hasNext()) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    private Object literalValue() {
        if (literal != null) {
            try {
                return literal.get();
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        }
        return null;
    }

}
