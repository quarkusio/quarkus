package io.quarkus.deployment.configuration.matching;

import org.wildfly.common.Assert;

import io.quarkus.deployment.configuration.definition.ClassDefinition;
import io.quarkus.deployment.configuration.definition.RootDefinition;

/**
 *
 */
public final class FieldContainer extends Container {
    private final Container parent;
    private final ClassDefinition.ClassMember member;

    public FieldContainer(final Container parent, final ClassDefinition.ClassMember member) {
        this.parent = parent;
        this.member = Assert.checkNotNullParam("member", member);
    }

    public Container getParent() {
        return parent;
    }

    public ClassDefinition.ClassMember getClassMember() {
        return member;
    }

    StringBuilder getCombinedName(final StringBuilder sb) {
        Container parent = getParent();
        if (parent != null) {
            parent.getCombinedName(sb);
        }
        final ClassDefinition enclosing = member.getEnclosingDefinition();
        if (enclosing instanceof RootDefinition) {
            RootDefinition rootDefinition = (RootDefinition) enclosing;
            String rootName = rootDefinition.getRootName();
            if (!rootName.isEmpty()) {
                sb.append(rootName.replace('.', ':'));
            }
        }
        if (sb.length() > 0) {
            sb.append(':');
        }
        sb.append(member.getName());
        return sb;
    }

    StringBuilder getPropertyName(final StringBuilder sb) {
        Container parent = getParent();
        if (parent != null) {
            parent.getPropertyName(sb);
        }
        final ClassDefinition enclosing = member.getEnclosingDefinition();
        if (enclosing instanceof RootDefinition) {
            RootDefinition rootDefinition = (RootDefinition) enclosing;
            String rootName = rootDefinition.getRootName();
            if (!rootName.isEmpty()) {
                sb.append(rootName);
            }
        }
        final String propertyName = member.getPropertyName();
        if (!propertyName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(propertyName);
        }
        return sb;
    }
}
