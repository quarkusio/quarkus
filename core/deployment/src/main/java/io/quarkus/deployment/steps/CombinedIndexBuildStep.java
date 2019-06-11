package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

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
