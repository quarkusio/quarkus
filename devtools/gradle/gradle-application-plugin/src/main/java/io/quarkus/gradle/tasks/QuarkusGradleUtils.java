package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import io.quarkus.bootstrap.util.IoUtils;

public class QuarkusGradleUtils {

    private static final String ERROR_COLLECTING_PROJECT_CLASSES = "Failed to collect project's classes in a temporary dir";

    public static SourceSetContainer getSourceSets(Project project) {
        return project.getExtensions().getByType(SourceSetContainer.class);
    }

    public static SourceSet getSourceSet(Project project, String sourceSetName) {
        return getSourceSets(project).getByName(sourceSetName);
    }

    public static String getClassesDir(SourceSet sourceSet, File tmpDir, boolean test) {
        return getClassesDir(sourceSet, tmpDir, true, test);
    }

    public static String getClassesDir(SourceSet sourceSet, File tmpDir, boolean populated, boolean test) {
        final FileCollection classesDirs = sourceSet.getOutput().getClassesDirs();
        final Set<File> classDirFiles = classesDirs.getFiles();
        if (classDirFiles.size() == 1) {
            return classesDirs.getAsPath();
        }
        final Set<Path> classesPaths = new HashSet<>(classDirFiles.size());
        classesDirs.forEach(f -> classesPaths.add(f.toPath()));
        final Path merged = mergeClassesDirs(classesPaths, tmpDir, populated, test);
        return merged == null ? null : merged.toString();
    }

    public static Path mergeClassesDirs(Collection<Path> classesDirs, File tmpDir, boolean populated, boolean test) {
        List<Path> existingClassesDirs = classesDirs.stream().filter(p -> Files.exists(p)).toList();

        if (existingClassesDirs.size() == 0) {
            return null;
        }

        if (existingClassesDirs.size() == 1) {
            return existingClassesDirs.get(0);
        }

        try {
            Path mergedClassesDir = tmpDir.toPath().resolve("quarkus-app-classes" + (test ? "-test" : ""));

            if (!populated) {
                return mergedClassesDir;
            }

            if (Files.exists(mergedClassesDir)) {
                IoUtils.recursiveDelete(mergedClassesDir);
            }

            for (Path classesDir : existingClassesDirs) {
                IoUtils.copy(classesDir, mergedClassesDir);
            }

            return mergedClassesDir;
        } catch (IOException e) {
            throw new UncheckedIOException(ERROR_COLLECTING_PROJECT_CLASSES, e);
        }
    }

}
