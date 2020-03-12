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
 * 
 */
final class ExpressionImpl implements Expression {

    static class VirtualMethodExpressionPartImpl extends ExpressionPartImpl implements VirtualMethodPart {

        private final List<Expression> parameters;

        VirtualMethodExpressionPartImpl(String name, List<Expression> parameters) {
            super(name, null);
            this.parameters = parameters;
        }

        public List<Expression> getParameters() {
            return parameters;
        }

        @Override
        public boolean isVirtualMethod() {
            return true;
        }

        @Override
        public VirtualMethodPart asVirtualMethod() {
            return this;
        }

        @Override
        public String getTypeInfo() {
            return toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(parameters);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            VirtualMethodExpressionPartImpl other = (VirtualMethodExpressionPartImpl) obj;
            return Objects.equals(parameters, other.parameters);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name).append("(");
            for (Iterator<Expression> iterator = parameters.iterator(); iterator.hasNext();) {
                Expression expression = iterator.next();
                builder.append(expression.toOriginalString());
                if (iterator.hasNext()) {
                    builder.append(",");
                }
            }
            builder.append(")");
            return builder.toString();
        }

    }

    static class ExpressionPartImpl implements Part {

        protected final String name;
        protected final String typeInfo;

        ExpressionPartImpl(String name, String typeInfo) {
            this.name = name;
            this.typeInfo = typeInfo;
        }

        public String getName() {
            return name;
        }

        public String getTypeInfo() {
            return typeInfo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, typeInfo);
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
            ExpressionPartImpl other = (ExpressionPartImpl) obj;
            return Objects.equals(name, other.name) && Objects.equals(typeInfo, other.typeInfo);
        }

        @Override
        public String toString() {
            return name;
        }

    }

    static final ExpressionImpl EMPTY = new ExpressionImpl(null, Collections.emptyList(), null, null);

    /**
     * 
     * @param value
     * @return a non-contextual expression
     */
    static ExpressionImpl from(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY;
        }
        return Parser.parseExpression(value, Collections.emptyMap(), Parser.SYNTHETIC_ORIGIN);
    }

    static ExpressionImpl literalFrom(String literal) {
        if (literal == null || literal.isEmpty()) {
            return EMPTY;
        }
        Object literalValue = LiteralSupport.getLiteralValue(literal);
        if (literalValue == null) {
            throw new IllegalArgumentException("Not a literal value: " + literal);
        }
        return literal(literal, literalValue, Parser.SYNTHETIC_ORIGIN);
    }

    static ExpressionImpl literal(String literal, Object value, Origin origin) {
        if (literal == null) {
            throw new IllegalArgumentException("Literal must not be null");
        }
        return new ExpressionImpl(null,
                Collections.singletonList(new ExpressionPartImpl(literal,
                        value != null
                                ? Expressions.TYPE_INFO_SEPARATOR + value.getClass().getName() + Expressions.TYPE_INFO_SEPARATOR
                                : null)),
                value, origin);
    }

    private final String namespace;
    private final List<Part> parts;
    private final CompletableFuture<Object> literal;
    private final Origin origin;

    ExpressionImpl(String namespace, List<Part> parts, Object literal, Origin origin) {
        this.namespace = namespace;
        this.parts = parts;
        this.literal = literal != Result.NOT_FOUND ? CompletableFuture.completedFuture(literal) : null;
        this.origin = origin;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<Part> getParts() {
        return parts;
    }

    @Override
    public boolean isLiteral() {
        return literal != null;
    }

    public CompletableFuture<Object> getLiteralValue() {
        return literal;
    }

    public Origin getOrigin() {
        return origin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toOriginalString());
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
        ExpressionImpl other = (ExpressionImpl) obj;
        return Objects.equals(toOriginalString(), other.toOriginalString());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Expression [namespace=").append(namespace).append(", parts=").append(parts).append(", literal=")
                .append(literalValue())
                .append("]");
        return builder.toString();
    }

    public String toOriginalString() {
        StringBuilder builder = new StringBuilder();
        if (namespace != null) {
            builder.append(namespace);
            builder.append(":");
        }
        for (Iterator<Part> iterator = parts.iterator(); iterator.hasNext();) {
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
