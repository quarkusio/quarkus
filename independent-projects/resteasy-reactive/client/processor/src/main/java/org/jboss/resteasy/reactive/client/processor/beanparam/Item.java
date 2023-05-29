package org.jboss.resteasy.reactive.client.processor.beanparam;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public abstract class Item {

    private final String fieldName;
    private final ItemType type;
    private final boolean encoded;
    private final ValueExtractor valueExtractor;

    public Item(String fieldName, ItemType type, boolean encoded, ValueExtractor valueExtractor) {
        this.fieldName = fieldName;
        this.type = type;
        this.encoded = encoded;
        this.valueExtractor = valueExtractor;
    }

    public String fieldName() {
        return fieldName;
    }

    public ItemType type() {
        return type;
    }

    public boolean isEncoded() {
        return encoded;
    }

    public ResultHandle extract(BytecodeCreator methodCreator, ResultHandle param) {
        return valueExtractor.extract(methodCreator, param);
    }
}
