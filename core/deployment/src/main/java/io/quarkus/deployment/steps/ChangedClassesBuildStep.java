package io.quarkus.deployment.steps;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ChangedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;

public class ChangedClassesBuildStep {

    private static volatile IndexView oldIndex;

    @BuildStep(onlyIf = IsDevelopment.class)
    ChangedClassesBuildItem changedClassesBuildItem(CombinedIndexBuildItem combinedIndexBuildItem,
            LiveReloadBuildItem liveReloadBuildItem) {
        IndexView currentIndex = combinedIndexBuildItem.getIndex();
        if (liveReloadBuildItem.getChangeInformation() == null) {
            oldIndex = currentIndex;
            return null;
        }
        Map<DotName, ClassInfo> changedClassesNewVersion = new HashMap<>();
        Map<DotName, ClassInfo> changedClassesOldVersion = new HashMap<>();
        Map<DotName, ClassInfo> deletedClasses = new HashMap<>();
        Map<DotName, ClassInfo> addedClasses = new HashMap<>();
        for (String added : liveReloadBuildItem.getChangeInformation().getAddedClasses()) {
            DotName name = DotName.createSimple(added);
            ClassInfo clazz = currentIndex.getClassByName(name);
            if (clazz == null) {
                //should never happen, but we bail out to be paranoid
                return null;
            }
            addedClasses.put(name, clazz);
        }
        for (String deleted : liveReloadBuildItem.getChangeInformation().getDeletedClasses()) {
            DotName name = DotName.createSimple(deleted);
            ClassInfo clazz = oldIndex.getClassByName(name);
            if (clazz == null) {
                //should never happen, but we bail out to be paranoid
                return null;
            }
            addedClasses.put(name, clazz);
        }
        for (String mod : liveReloadBuildItem.getChangeInformation().getChangedClasses()) {
            DotName name = DotName.createSimple(mod);
            ClassInfo clazz = oldIndex.getClassByName(name);
            if (clazz == null) {
                //should never happen, but we bail out to be paranoid
                return null;
            }
            changedClassesOldVersion.put(name, clazz);
            clazz = currentIndex.getClassByName(name);
            if (clazz == null) {
                //should never happen, but we bail out to be paranoid
                return null;
            }
            changedClassesNewVersion.put(name, clazz);
        }

        oldIndex = currentIndex;
        return new ChangedClassesBuildItem(changedClassesNewVersion, changedClassesOldVersion, deletedClasses, addedClasses);
    }

}
