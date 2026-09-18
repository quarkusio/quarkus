package io.quarkus.qute.deployment;

import java.net.URI;
import java.util.Optional;

import io.quarkus.qute.Expression;
import io.quarkus.qute.TemplateNode.Origin;
import io.quarkus.qute.Variant;

/**
 * The origin of an expression narrowed to one of its parts, so that an error can point to the column of the part.
 */
final class PartOrigin implements Origin {

    /**
     * @return the origin of the part if its position is known, the origin of the expression otherwise
     */
    static Origin of(Expression expression, Expression.Part part) {
        Origin origin = expression.getOrigin();
        if (part == null || part.getLineCharacterStart() < 0 || origin.isSynthetic()) {
            return origin;
        }
        return new PartOrigin(origin, part.getLineCharacterStart(), part.getLineCharacterStart() + part.getName().length());
    }

    private final Origin origin;
    private final int lineCharacterStart;
    private final int lineCharacterEnd;

    private PartOrigin(Origin origin, int lineCharacterStart, int lineCharacterEnd) {
        this.origin = origin;
        this.lineCharacterStart = lineCharacterStart;
        this.lineCharacterEnd = lineCharacterEnd;
    }

    @Override
    public int getLine() {
        return origin.getLine();
    }

    @Override
    public int getLineCharacterStart() {
        return lineCharacterStart;
    }

    @Override
    public int getLineCharacterEnd() {
        return lineCharacterEnd;
    }

    @Override
    public String getTemplateId() {
        return origin.getTemplateId();
    }

    @Override
    public String getTemplateGeneratedId() {
        return origin.getTemplateGeneratedId();
    }

    @Override
    public Optional<Variant> getVariant() {
        return origin.getVariant();
    }

    @Override
    public Optional<URI> getSourceUri() {
        return origin.getSourceUri();
    }

    @Override
    public String toString() {
        return origin.toString();
    }
}
