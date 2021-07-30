package io.quarkus.qute;

import io.quarkus.qute.TemplateNode.Origin;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class ExpressionImpl implements Expression {

    static final ExpressionImpl EMPTY = new ExpressionImpl(0, null, Collections.emptyList(), Results.NotFound.EMPTY, null);

    /**
     * 
     * @param value
     * @return a new expression
     */
    static ExpressionImpl from(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY;
        }
        return Parser.parseExpression(ExpressionImpl::syntheticId, value, Scope.EMPTY, Parser.SYNTHETIC_ORIGIN);
    }

    static ExpressionImpl literalFrom(int id, String literal) {
        if (literal == null || literal.isEmpty()) {
            return EMPTY;
        }
        Object literalValue = LiteralSupport.getLiteralValue(literal);
        return literal(id, literal, literalValue, Parser.SYNTHETIC_ORIGIN);
    }

    static ExpressionImpl literal(int id, String literal, Object value, Origin origin) {
        if (literal == null) {
            throw new IllegalArgumentException("Literal must not be null");
        }
        return new ExpressionImpl(id, null,
                Collections.singletonList(new PartImpl(literal,
                        value != null ? Expressions.typeInfoFrom(value.getClass().getName()) : null)),
                value, origin);
    }

    static Integer syntheticId() {
        return -1;
    }

    private final int id;
    private final String namespace;
    private final List<Part> parts;
    private final CompletedStage<Object> literal;
    private final Origin origin;

    ExpressionImpl(int id, String namespace, List<Part> parts, Object literal, Origin origin) {
        this.id = id;
        this.namespace = namespace;
        this.parts = parts;
        this.literal = literal != Results.NotFound.EMPTY ? CompletedStage.of(literal) : null;
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
        return literal != null ? literal.toCompletableFuture() : null;
    }

    @Override
    public Object getLiteral() {
        return literal != null ? literal.get() : null;
    }

    @Override
    public CompletionStage<Object> asLiteral() {
        if (literal == null) {
            throw new IllegalStateException("Expression is not a literal: " + toString());
        }
        return literal;
    }

    public Origin getOrigin() {
        return origin;
    }

    @Override
    public int getGeneratedId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(toOriginalString());
        result = prime * result + Objects.hashCode(origin);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExpressionImpl other = (ExpressionImpl) obj;
        return Objects.equals(toOriginalString(), other.toOriginalString()) && Objects.equals(origin, other.origin);
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
            return literal.get();
        }
        return null;
    }

    static class VirtualMethodPartImpl extends PartImpl implements VirtualMethodPart {

        private final List<Expression> parameters;

        VirtualMethodPartImpl(String name, List<Expression> parameters, String lastPartHint) {
            super(name, buildTypeInfo(name, parameters, lastPartHint));
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
            VirtualMethodPartImpl other = (VirtualMethodPartImpl) obj;
            return Objects.equals(parameters, other.parameters);
        }

        @Override
        public String toString() {
            return buildTypeInfo(name, parameters, null);
        }

        private static String buildTypeInfo(String name, List<Expression> parameters, String lastPartHint) {
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
            if (lastPartHint != null) {
                builder.append(lastPartHint);
            }
            return builder.toString();
        }

    }

    static class PartImpl implements Part {

        protected final String name;
        protected final String typeInfo;
        protected volatile ValueResolver cachedResolver;

        PartImpl(String name, String typeInfo) {
            this.name = name;
            this.typeInfo = typeInfo;
        }

        public String getName() {
            return name;
        }

        public String getTypeInfo() {
            return typeInfo;
        }

        void setCachedResolver(ValueResolver resolver) {
            ValueResolver last = this.cachedResolver;
            if (last != null) {
                return;
            }
            synchronized (this) {
                if (this.cachedResolver == null) {
                    this.cachedResolver = resolver;
                }
            }
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
            PartImpl other = (PartImpl) obj;
            return Objects.equals(name, other.name) && Objects.equals(typeInfo, other.typeInfo);
        }

        @Override
        public String toString() {
            return name;
        }

    }
}
