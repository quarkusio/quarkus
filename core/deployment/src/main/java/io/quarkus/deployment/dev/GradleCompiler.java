package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.objectweb.asm.ClassReader;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.paths.PathCollection;

public class GradleCompiler implements Closeable {

    ProjectConnection projectConnection;

    public GradleCompiler(CuratedApplication application,
            List<CompilationProvider> compilationProviders,
            DevModeContext context) {
        var startTime = System.currentTimeMillis();
        var projectDir = new File(context.getApplicationRoot().getProjectDirectory());
        var connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
        projectConnection = connector.connect();
        var connectedToProject = System.currentTimeMillis();
        System.out.println("Connected to Gradle project took "
                + (connectedToProject - startTime) + "ms");
        var model = projectConnection.getModel(org.gradle.tooling.model.GradleProject.class);
        var modelTime = System.currentTimeMillis();
        System.out.println("Retrieved Gradle model in "
                + (modelTime - connectedToProject) + "ms");
        // model.getTasks().forEach(t -> System.out.println("Task: " + t.getName() + " - " + t.getDescription()));
        var classesTask = model.getTasks().stream()
                .filter(t -> "classes".equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No 'classes' task found in Gradle project"));
    }

    public Set<String> allHandledExtensions() {
        return Set.of(".java");
    }

    public void compile(String sourceDir, Map<String, Set<File>> extensionToChangedFiles) {
        var startTime = System.currentTimeMillis();
        var newBuild = projectConnection.newBuild();
        var createdBuild = System.currentTimeMillis();
        System.out.println("Created new Gradle build in "
                + (createdBuild - startTime) + "ms");
        // exclude quarkusGenerateCode
        // quarkusGenerateDevAppModel
        // quarkusGenerateCodeDev
        newBuild
                // .forTasks("classes")
                .withArguments("classes", "-x", "quarkusGenerateCode",
                        "-x", "quarkusGenerateDevAppModel", "-x", "quarkusGenerateCodeDev")
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .run();
        var runTime = System.currentTimeMillis();
        System.out.println("Ran Gradle task in "
                + (runTime - createdBuild) + "ms");
    }

    public Path findSourcePath(Path classFilePath, PathCollection sourcePaths, String classesPath) {
        Path sourceFilePath;
        final RuntimeUpdatesClassVisitor visitor = new RuntimeUpdatesClassVisitor(sourcePaths, classesPath);
        try (final InputStream inputStream = Files.newInputStream(classFilePath)) {
            final ClassReader reader = new ClassReader(inputStream);
            reader.accept(visitor, 0);
            sourceFilePath = visitor.getSourceFileForClass(classFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sourceFilePath;
    }

    @Override
    public void close() throws IOException {
        projectConnection.close();
    }
}
