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

    public boolean isChanged() {
        return !changedClasses.isEmpty() || !deletedClasses.isEmpty() || !addedClasses.isEmpty();
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

    private String toName(Path moduleClassesPath, Path classFilePath) {
        String cf = moduleClassesPath.relativize(classFilePath).toString()
                .replace(moduleClassesPath.getFileSystem().getSeparator(), ".");
        return cf.substring(0, cf.length() - ".class".length());
    }
}
