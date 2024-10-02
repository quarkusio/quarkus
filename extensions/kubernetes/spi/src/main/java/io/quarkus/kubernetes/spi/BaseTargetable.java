package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

class BaseTargetable extends MultiBuildItem implements Targetable {
    private final String target;

    protected BaseTargetable(String target) {
        this.target = target;
    }

    @Override
    public String getTarget() {
        return target;
    }
}
