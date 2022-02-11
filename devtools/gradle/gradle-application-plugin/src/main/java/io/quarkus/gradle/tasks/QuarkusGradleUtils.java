package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.bootstrap.util.IoUtils;

public class QuarkusGradleUtils {

    private static final String ERROR_COLLECTING_PROJECT_CLASSES = "Failed to collect project's classes in a temporary dir";

    public static SourceSet getSourceSet(Project project, String sourceSetName) {
        final JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaConvention == null) {
            throw new IllegalArgumentException("The project does not include the Java plugin");
        }
        return javaConvention.getSourceSets().getByName(sourceSetName);
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
        Path classesDir = null;
        final Iterator<Path> i = classesDirs.iterator();
        int dirCount = 0;
        while (i.hasNext()) {
            final Path next = i.next();
            if (!Files.exists(next)) {
                continue;
            }
            try {
                switch (dirCount++) {
                    case 0:
                        classesDir = next;
                        break;
                    case 1:
                        //there does not seem to be any sane way of dealing with multiple output dirs, as there does not seem
                        //to be a way to map them. We will need to address this at some point, but for now we just stick them
                        //all in a temp dir
                        final Path tmpClassesDir = tmpDir.toPath().resolve("quarkus-app-classes" + (test ? "-test" : ""));
                        if (!populated) {
                            return tmpClassesDir;
                        }
                        if (Files.exists(tmpClassesDir)) {
                            IoUtils.recursiveDelete(tmpClassesDir);
                        }
                        IoUtils.copy(classesDir, tmpClassesDir);
                        classesDir = tmpClassesDir;
                    default:
                        IoUtils.copy(next, classesDir);

                }
            } catch (IOException e) {
                throw new UncheckedIOException(ERROR_COLLECTING_PROJECT_CLASSES, e);
            }
        }
        if (classesDir == null) {
            return null;
        }
        return classesDir;
    }

}
