package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.index.IndexWrapper;
import io.quarkus.deployment.index.LazyIndexer;
import io.quarkus.deployment.index.PersistentClassIndex;

public class CombinedIndexBuildStep {

    @BuildStep
    CombinedIndexBuildItem build(ApplicationArchivesBuildItem archives,
            List<AdditionalIndexedClassesBuildItem> additionalIndexedClassesItems,
            LiveReloadBuildItem liveReloadBuildItem) {
        List<IndexView> archiveIndexes = new ArrayList<>();

        for (ApplicationArchive i : archives.getAllArchives()) {
            archiveIndexes.add(i.getIndex());
        }

        CompositeIndex archivesIndex = CompositeIndex.create(archiveIndexes);

        LazyIndexer indexer = new LazyIndexer(Thread.currentThread().getContextClassLoader(), archivesIndex);
        for (AdditionalIndexedClassesBuildItem additionalIndexedClasses : additionalIndexedClassesItems) {
            indexer.addAll(additionalIndexedClasses.getClassesToIndex());
        }
        LazyIndexer.Result result = indexer.complete();

        PersistentClassIndex index = liveReloadBuildItem.getContextObject(PersistentClassIndex.class);
        if (index == null) {
            index = new PersistentClassIndex();
            liveReloadBuildItem.setContextObject(PersistentClassIndex.class, index);
        }

        Map<DotName, Optional<ClassInfo>> additionalClasses = index.getAdditionalClasses();
        for (DotName knownMissingClass : result.missingAnnotations()) {
            additionalClasses.put(knownMissingClass, Optional.empty());
        }

        CompositeIndex compositeIndex = CompositeIndex.create(archivesIndex, result.index());
        RuntimeUpdatesProcessor.setLastStartIndex(compositeIndex);
        return new CombinedIndexBuildItem(compositeIndex,
                new IndexWrapper(compositeIndex, Thread.currentThread().getContextClassLoader(), index));
    }

}
