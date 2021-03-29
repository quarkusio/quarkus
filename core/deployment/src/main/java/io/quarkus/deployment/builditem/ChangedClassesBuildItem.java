package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represents the differences between classes in a dev mode restart.
 *
 * This can be used to avoid repeating work on restart, e.g. re-using
 * old proxy definitions if nothing has changed for a given class.
 *
 * This will not always be present, it must be injected as an
 * optional dependency.
 *
 * This will never be generated if the previous restart was a failure
 * to avoid issues with inconsistent application state.
 */
public class ChangedClassesBuildItem extends SimpleBuildItem {

    private final Map<DotName, ClassInfo> changedClassesNewVersion;
    private final Map<DotName, ClassInfo> changedClassesOldVersion;
    private final Map<DotName, ClassInfo> deletedClasses;
    private final Map<DotName, ClassInfo> addedClasses;

    public ChangedClassesBuildItem(Map<DotName, ClassInfo> changedClassesNewVersion,
            Map<DotName, ClassInfo> changedClassesOldVersion, Map<DotName, ClassInfo> deletedClasses,
            Map<DotName, ClassInfo> addedClasses) {
        this.changedClassesNewVersion = changedClassesNewVersion;
        this.changedClassesOldVersion = changedClassesOldVersion;
        this.deletedClasses = deletedClasses;
        this.addedClasses = addedClasses;
    }

    public Map<DotName, ClassInfo> getChangedClassesNewVersion() {
        return Collections.unmodifiableMap(changedClassesNewVersion);
    }

    public Map<DotName, ClassInfo> getChangedClassesOldVersion() {
        return Collections.unmodifiableMap(changedClassesOldVersion);
    }

    public Map<DotName, ClassInfo> getDeletedClasses() {
        return Collections.unmodifiableMap(deletedClasses);
    }

    public Map<DotName, ClassInfo> getAddedClasses() {
        return Collections.unmodifiableMap(addedClasses);
    }
}
