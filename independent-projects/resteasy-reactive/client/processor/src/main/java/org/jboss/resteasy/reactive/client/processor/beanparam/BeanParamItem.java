package org.jboss.resteasy.reactive.client.processor.beanparam;

import java.util.List;

public class BeanParamItem extends Item {
    private final List<Item> items;

    public List<Item> items() {
        return items;
    }

    public BeanParamItem(List<Item> items, ValueExtractor extractor) {
        super(ItemType.BEAN_PARAM, extractor);
        this.items = items;
    }
}
