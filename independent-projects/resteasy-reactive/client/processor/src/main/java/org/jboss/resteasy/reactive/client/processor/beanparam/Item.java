package org.jboss.resteasy.reactive.client.processor.beanparam;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public abstract class Item {

    private final ItemType type;
    private final ValueExtractor valueExtractor;

    public Item(ItemType type, ValueExtractor valueExtractor) {
        this.type = type;
        this.valueExtractor = valueExtractor;
    }

    public ItemType type() {
        return type;
    }

    public ResultHandle extract(BytecodeCreator methodCreator, ResultHandle param) {
        return valueExtractor.extract(methodCreator, param);
    }

}
