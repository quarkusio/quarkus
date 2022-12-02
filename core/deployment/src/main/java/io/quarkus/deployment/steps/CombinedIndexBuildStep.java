package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.index.IndexWrapper;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.index.PersistentClassIndex;

public class CombinedIndexBuildStep {

    @BuildStep
    CombinedIndexBuildItem build(ApplicationArchivesBuildItem archives,
            List<AdditionalIndexedClassesBuildItem> additionalIndexedClassesItems,
            LiveReloadBuildItem liveReloadBuildItem) {
        List<IndexView> archiveIndexes = new ArrayList<>();

        for (ApplicationArchive i : archives.getAllApplicationArchives()) {
            archiveIndexes.add(i.getIndex());
        }

        CompositeIndex archivesIndex = CompositeIndex.create(archiveIndexes);

        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        Set<DotName> knownMissingClasses = new HashSet<>();

        for (AdditionalIndexedClassesBuildItem additionalIndexedClasses : additionalIndexedClassesItems) {
            for (String classToIndex : additionalIndexedClasses.getClassesToIndex()) {
                IndexingUtil.indexClass(classToIndex, indexer, archivesIndex, additionalIndex,
                        knownMissingClasses, Thread.currentThread().getContextClassLoader());
            }
        }

        PersistentClassIndex index = liveReloadBuildItem.getContextObject(PersistentClassIndex.class);
        if (index == null) {
            index = new PersistentClassIndex();
            liveReloadBuildItem.setContextObject(PersistentClassIndex.class, index);
        }

        Map<DotName, Optional<ClassInfo>> additionalClasses = index.getAdditionalClasses();
        for (DotName knownMissingClass : knownMissingClasses) {
            additionalClasses.put(knownMissingClass, Optional.empty());
        }

        CompositeIndex compositeIndex = CompositeIndex.create(archivesIndex, indexer.complete());
        RuntimeUpdatesProcessor.setLastStartIndex(compositeIndex);
        return new CombinedIndexBuildItem(compositeIndex,
                new IndexWrapper(compositeIndex, Thread.currentThread().getContextClassLoader(), index));
    }

}
