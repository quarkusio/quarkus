package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.util.IoUtils;

public class QuarkusGradleUtils {

    private static final String ERROR_COLLECTING_PROJECT_CLASSES = "Failed to collect project's classes in a temporary dir";

    public static Path serializeAppModel(ApplicationModel appModel, Task context, boolean test) throws IOException {
        final Path serializedModel = context.getTemporaryDir().toPath()
                .resolve("quarkus-app" + (test ? "-test" : "") + "-model.dat");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(appModel);
        }
        return serializedModel;
    }

    public static SourceSet getSourceSet(Project project, String sourceSetName) {
        final JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaConvention == null) {
            throw new IllegalArgumentException("The project does not include the Java plugin");
        }
        return javaConvention.getSourceSets().getByName(sourceSetName);
    }

    public static PathsCollection getOutputPaths(Project project) {
        final SourceSet mainSourceSet = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME);
        final PathsCollection.Builder builder = PathsCollection.builder();
        mainSourceSet.getOutput().getClassesDirs().filter(f -> f.exists()).forEach(f -> builder.add(f.toPath()));
        final File resourcesDir = mainSourceSet.getOutput().getResourcesDir();
        if (resourcesDir != null && resourcesDir.exists()) {
            final Path p = resourcesDir.toPath();
            if (!builder.contains(p)) {
                builder.add(p);
            }
        }
        return builder.build();
    }

    public static String getClassesDir(SourceSet sourceSet, File tmpDir, boolean test) {
        return getClassesDir(sourceSet, tmpDir, true, test);
    }

    public static String getClassesDir(SourceSet sourceSet, File tmpDir, boolean populated, boolean test) {
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
                        final Path tmpClassesDir = tmpDir.toPath().resolve("quarkus-app-classes" + (test ? "-test" : ""));
                        if (!populated) {
                            return tmpClassesDir.toString();
                        }
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
            return null;
        }
        return classesDir.toString();
    }

}
