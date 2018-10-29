package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.FieldInfo;

public final class ReflectiveFieldBuildItem extends MultiBuildItem {

    final FieldInfo field;

    public ReflectiveFieldBuildItem(FieldInfo field) {
        this.field = field;
    }

    public FieldInfo getField() {
        return field;
    }
}
