package io.quarkus.bootstrap.app;

import java.util.Collections;
import java.util.Set;

public class ClassChangeInformation {

    private final Set<String> changedClasses;
    private final Set<String> deletedClasses;
    private final Set<String> addedClasses;

    public ClassChangeInformation(Set<String> changedClasses, Set<String> deletedClasses, Set<String> addedClasses) {
        this.changedClasses = changedClasses;
        this.deletedClasses = deletedClasses;
        this.addedClasses = addedClasses;
    }

    public Set<String> getChangedClasses() {
        return Collections.unmodifiableSet(changedClasses);
    }

    public Set<String> getDeletedClasses() {
        return Collections.unmodifiableSet(deletedClasses);
    }

    public Set<String> getAddedClasses() {
        return Collections.unmodifiableSet(addedClasses);
    }
}
