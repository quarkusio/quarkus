package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.jandex.IndexView;

public final class CombinedIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;

    public CombinedIndexBuildItem(IndexView index) {
        this.index = index;
    }

    public IndexView getIndex() {
        return index;
    }
}
