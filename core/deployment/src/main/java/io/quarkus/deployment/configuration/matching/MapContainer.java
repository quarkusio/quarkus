package io.quarkus.deployment.configuration.matching;

import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.smallrye.common.constraint.Assert;

/**
 * A map container.
 */
public final class MapContainer extends Container {
    private final Container parent;
    private final ClassDefinition.ClassMember mapMember;

    public MapContainer(final Container parent, final ClassDefinition.ClassMember mapMember) {
        this.parent = Assert.checkNotNullParam("parent", parent);
        this.mapMember = mapMember;
    }

    @Override
    public ClassDefinition.ClassMember getClassMember() {
        return mapMember;
    }

    @Override
    public Container getParent() {
        return parent;
    }

    @Override
    StringBuilder getCombinedName(final StringBuilder sb) {
        // maps always have a parent
        getParent().getCombinedName(sb);
        if (sb.length() > 0) {
            sb.append(':');
        }
        sb.append('*');
        return sb;
    }

    @Override
    StringBuilder getPropertyName(final StringBuilder sb) {
        // maps always have a parent
        getParent().getPropertyName(sb);
        if (sb.length() > 0) {
            sb.append('.');
        }
        sb.append('*');
        return sb;
    }
}
