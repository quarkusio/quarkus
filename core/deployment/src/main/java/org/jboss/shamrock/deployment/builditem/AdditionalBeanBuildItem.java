package org.jboss.shamrock.deployment.builditem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

public final class AdditionalBeanBuildItem extends MultiBuildItem {

    private final List<String> beanNames;

    public AdditionalBeanBuildItem(String... beanNames) {
        this.beanNames = Arrays.asList(beanNames);
    }

    public AdditionalBeanBuildItem(Class... beanClasss) {
        beanNames = new ArrayList<>(beanClasss.length);
        for (Class i : beanClasss) {
            beanNames.add(i.getName());
        }
    }

    public List<String> getBeanNames() {
        return Collections.unmodifiableList(beanNames);
    }
}
