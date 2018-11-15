package org.jboss.shamrock.deployment.steps;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.ApplicationArchive;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;

public class CombinedIndexBuildStep {

    @BuildStep
    CombinedIndexBuildItem build(ApplicationArchivesBuildItem archives) {
        List<IndexView> allIndexes = new ArrayList<>();
        for (ApplicationArchive i : archives.getAllApplicationArchives()) {
            allIndexes.add(i.getIndex());
        }
        return new CombinedIndexBuildItem(CompositeIndex.create(allIndexes));
    }

}
