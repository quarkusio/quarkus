package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.util.IoUtils;

public class QuarkusGradleUtils {

    private static final String ERROR_COLLECTING_PROJECT_CLASSES = "Failed to collect project's classes in a temporary dir";

    public static Path serializeAppModel(final AppModel appModel, AbstractTask context) throws IOException {
        final Path serializedModel = context.getTemporaryDir().toPath().resolve("quarkus-app-model.dat");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(appModel);
        }
        return serializedModel;
    }

    public static String getClassesDir(SourceSet sourceSet, AbstractTask context) {
        final Set<String> sourcePaths = new HashSet<>();
        for (File sourceDir : sourceSet.getAllJava().getSrcDirs()) {
            sourcePaths.add(sourceDir.getAbsolutePath());
        }

        FileCollection classesDirs = sourceSet.getOutput().getClassesDirs();
        Set<File> classDirFiles = classesDirs.getFiles();
        if (classDirFiles.size() == 1) {
            return classesDirs.getAsPath();
        }

        Path classesDir = null;
        final Iterator<File> i = classDirFiles.iterator();
        int dirCount = 0;
        while (i.hasNext()) {
            final File next = i.next();
            if (!next.exists()) {
                continue;
            }
            try {
                switch (dirCount++) {
                    case 0:
                        classesDir = next.toPath();
                        break;
                    case 1:
                        //there does not seem to be any sane way of dealing with multiple output dirs, as there does not seem
                        //to be a way to map them. We will need to address this at some point, but for now we just stick them
                        //all in a temp dir
                        final Path tmpClassesDir = context.getTemporaryDir().toPath().resolve("quarkus-app-classes");
                        if (Files.exists(tmpClassesDir)) {
                            IoUtils.recursiveDelete(tmpClassesDir);
                        }
                        IoUtils.copy(classesDir, tmpClassesDir);
                        classesDir = tmpClassesDir;
                    default:
                        IoUtils.copy(next.toPath(), classesDir);

                }
            } catch (IOException e) {
                throw new UncheckedIOException(ERROR_COLLECTING_PROJECT_CLASSES, e);
            }
        }
        if (classesDir == null) {
            throw new IllegalStateException("Failed to locate classes directory in the project");
        }
        return classesDir.toString();
    }
}
