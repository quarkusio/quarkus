package org.jboss.resteasy.reactive.client.processor.beanparam;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public abstract class Item {

    private final String fieldName;
    private final ItemType type;
    private final ValueExtractor valueExtractor;

    public Item(String fieldName, ItemType type, ValueExtractor valueExtractor) {
        this.fieldName = fieldName;
        this.type = type;
        this.valueExtractor = valueExtractor;
    }

    public String fieldName() {
        return fieldName;
    }

    public ItemType type() {
        return type;
    }

    public ResultHandle extract(BytecodeCreator methodCreator, ResultHandle param) {
        return valueExtractor.extract(methodCreator, param);
    }
}
