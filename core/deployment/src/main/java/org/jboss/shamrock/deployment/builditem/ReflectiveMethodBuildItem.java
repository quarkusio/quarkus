package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.MethodInfo;

public final class ReflectiveMethodBuildItem extends MultiBuildItem {

    final MethodInfo method;

    public ReflectiveMethodBuildItem(MethodInfo method) {
        this.method = method;
    }

    public MethodInfo getMethod() {
        return method;
    }
}
