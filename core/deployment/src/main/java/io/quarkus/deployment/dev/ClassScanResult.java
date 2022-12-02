package io.quarkus.deployment.dev;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ClassScanResult {
    final Set<Path> changedClasses = new HashSet<>();
    final Set<Path> deletedClasses = new HashSet<>();
    final Set<Path> addedClasses = new HashSet<>();
    final Set<String> changedClassNames = new HashSet<>();
    final Set<String> deletedClassNames = new HashSet<>();
    final Set<String> addedClassNames = new HashSet<>();
    boolean compilationHappened;

    public boolean isChanged() {
        return !changedClasses.isEmpty() || !deletedClasses.isEmpty() || !addedClasses.isEmpty() || compilationHappened;
    }

    public static ClassScanResult merge(ClassScanResult m1, ClassScanResult m2) {
        if (m1 == null) {
            return m2;
        }
        if (m2 == null) {
            return m1;
        }
        ClassScanResult ret = new ClassScanResult();
        ret.changedClasses.addAll(m1.changedClasses);
        ret.deletedClasses.addAll(m1.deletedClasses);
        ret.addedClasses.addAll(m1.addedClasses);
        ret.changedClassNames.addAll(m1.changedClassNames);
        ret.deletedClassNames.addAll(m1.deletedClassNames);
        ret.addedClassNames.addAll(m1.addedClassNames);
        ret.changedClasses.addAll(m2.changedClasses);
        ret.deletedClasses.addAll(m2.deletedClasses);
        ret.addedClasses.addAll(m2.addedClasses);
        ret.changedClassNames.addAll(m2.changedClassNames);
        ret.deletedClassNames.addAll(m2.deletedClassNames);
        ret.addedClassNames.addAll(m2.addedClassNames);
        ret.compilationHappened = m1.compilationHappened | m2.compilationHappened;
        return ret;
    }

    public void addDeletedClass(Path moduleClassesPath, Path classFilePath) {
        deletedClasses.add(classFilePath);
        deletedClassNames.add(toName(moduleClassesPath, classFilePath));
    }

    public void addChangedClass(Path moduleClassesPath, Path classFilePath) {
        changedClasses.add(classFilePath);
        changedClassNames.add(toName(moduleClassesPath, classFilePath));
    }

    public void addAddedClass(Path moduleClassesPath, Path classFilePath) {
        addedClasses.add(classFilePath);
        addedClassNames.add(toName(moduleClassesPath, classFilePath));
    }

    public Set<String> getChangedClassNames() {
        return changedClassNames;
    }

    public Set<Path> getChangedClasses() {
        return changedClasses;
    }

    public Set<Path> getDeletedClasses() {
        return deletedClasses;
    }

    public Set<Path> getAddedClasses() {
        return addedClasses;
    }

    public Set<String> getDeletedClassNames() {
        return deletedClassNames;
    }

    public Set<String> getAddedClassNames() {
        return addedClassNames;
    }

    public boolean isCompilationHappened() {
        return compilationHappened;
    }

    private String toName(Path moduleClassesPath, Path classFilePath) {
        String cf = moduleClassesPath.relativize(classFilePath).toString()
                .replace(moduleClassesPath.getFileSystem().getSeparator(), ".");
        return cf.substring(0, cf.length() - ".class".length());
    }

}
