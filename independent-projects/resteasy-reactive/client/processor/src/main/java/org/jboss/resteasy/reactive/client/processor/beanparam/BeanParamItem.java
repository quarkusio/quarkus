package org.jboss.resteasy.reactive.client.processor.beanparam;

import java.util.List;

public class BeanParamItem extends Item {
    private final List<Item> items;
    private final String className;

    public String className() {
        return className;
    }

    public List<Item> items() {
        return items;
    }

    public BeanParamItem(List<Item> items, String className, ValueExtractor extractor) {
        super(ItemType.BEAN_PARAM, extractor);
        this.items = items;
        this.className = className;
    }
}
