package io.quarkus.gradle.application.internal.plugin;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.SourceFolder;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

/**
 * Exposes Quarkus generated roots to standard IDE models without adding them to shared Java source sets.
 */
final class IdeGeneratedSourceWiring {

    private IdeGeneratedSourceWiring() {
    }

    static void wire(Project project, Provider<Directory> generatedMainSources,
            Provider<Directory> generatedTestSources) {
        File mainDirectory = generatedMainSources.get().getAsFile();
        File testDirectory = generatedTestSources.get().getAsFile();
        String mainPath = project.relativePath(mainDirectory);
        String testPath = project.relativePath(testDirectory);
        project.getPlugins().withType(IdeaPlugin.class,
                plugin -> wireIdea(plugin.getModel(), mainDirectory, testDirectory));
        project.getPlugins().withType(EclipsePlugin.class,
                ignored -> wireEclipse(project, mainPath, mainDirectory, testPath, testDirectory));
    }

    private static void wireIdea(IdeaModel idea, File generatedMainSources, File generatedTestSources) {
        IdeaModule module = idea.getModule();
        module.getSourceDirs().add(generatedMainSources);
        module.getTestSources().from(generatedTestSources);
        module.getGeneratedSourceDirs().add(generatedMainSources);
        module.getGeneratedSourceDirs().add(generatedTestSources);
    }

    private static void wireEclipse(Project project, String mainPath, File generatedMainSources,
            String testPath, File generatedTestSources) {
        EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
        eclipse.getClasspath().getFile().whenMerged(classpath -> {
            addSourceFolder((Classpath) classpath, mainPath, generatedMainSources);
            addSourceFolder((Classpath) classpath, testPath, generatedTestSources);
        });
    }

    private static void addSourceFolder(Classpath classpath, String path, File directory) {
        if (classpath.getEntries().stream().noneMatch(entry -> entry instanceof SourceFolder source
                && source.getPath().equals(path))) {
            SourceFolder sourceFolder = new SourceFolder(path, null);
            sourceFolder.setDir(directory);
            sourceFolder.setName(directory.getName());
            classpath.getEntries().add(sourceFolder);
        }
    }
}
