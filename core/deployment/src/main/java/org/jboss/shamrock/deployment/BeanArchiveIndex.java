package org.jboss.shamrock.deployment;

import org.jboss.jandex.IndexView;

public class BeanArchiveIndex {

    private IndexView index;


    public IndexView getIndex() {
        return index;
    }

    void setIndex(IndexView index) {
        this.index = index;
    }
}
