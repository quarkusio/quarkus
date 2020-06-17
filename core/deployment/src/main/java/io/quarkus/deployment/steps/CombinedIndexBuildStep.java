package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.index.IndexingUtil;

public class CombinedIndexBuildStep {

    @BuildStep
    CombinedIndexBuildItem build(ApplicationArchivesBuildItem archives,
            List<AdditionalIndexedClassesBuildItem> additionalIndexedClassesItems) {
        List<IndexView> archiveIndexes = new ArrayList<>();

        for (ApplicationArchive i : archives.getAllApplicationArchives()) {
            archiveIndexes.add(i.getIndex());
        }

        CompositeIndex archivesIndex = CompositeIndex.create(archiveIndexes);

        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();

        for (AdditionalIndexedClassesBuildItem additionalIndexedClasses : additionalIndexedClassesItems) {
            for (String classToIndex : additionalIndexedClasses.getClassesToIndex()) {
                IndexingUtil.indexClass(classToIndex, indexer, archivesIndex, additionalIndex,
                        Thread.currentThread().getContextClassLoader());
            }
        }

        return new CombinedIndexBuildItem(CompositeIndex.create(archivesIndex, indexer.complete()));
    }

}
