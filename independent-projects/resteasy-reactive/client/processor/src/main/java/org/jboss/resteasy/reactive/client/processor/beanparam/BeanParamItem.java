package org.jboss.resteasy.reactive.client.processor.beanparam;

import java.util.List;

public class BeanParamItem extends Item {
    private final List<Item> items;
    private final String className;

    public BeanParamItem(String fieldName, List<Item> items, String className, ValueExtractor extractor) {
        super(fieldName, ItemType.BEAN_PARAM, false, extractor);
        this.items = items;
        this.className = className;
    }

    public String className() {
        return className;
    }

    public List<Item> items() {
        return items;
    }
}
