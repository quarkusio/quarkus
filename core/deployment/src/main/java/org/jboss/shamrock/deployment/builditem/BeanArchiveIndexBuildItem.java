package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.jandex.IndexView;

public final class BeanArchiveIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;

    public BeanArchiveIndexBuildItem(IndexView index) {
        this.index = index;
    }


    public IndexView getIndex() {
        return index;
    }

}
