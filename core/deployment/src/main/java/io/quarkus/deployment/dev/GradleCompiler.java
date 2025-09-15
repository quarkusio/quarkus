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

        // Connect to the Gradle project
        projectConnection = connector.connect();
        var connectedToProject = System.currentTimeMillis();
        System.out.printf("Connecting to Gradle project took %s ms%n", +connectedToProject - startTime);

        // Test model retrieval
        var model = projectConnection.getModel(org.gradle.tooling.model.GradleProject.class);
        var modelTime = System.currentTimeMillis();
        System.out.printf("Retrieved Gradle model in %s ms%n", modelTime - connectedToProject);

        // Make sure we have the 'classes' task
        model.getTasks().stream()
                .filter(t -> "classes".equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No 'classes' task found in Gradle project"));
    }

    // For now, only handle Java files
    public Set<String> allHandledExtensions() {
        return Set.of(".java");
    }

    public void compile(String sourceDir, Map<String, Set<File>> extensionToChangedFiles) {
        var startTime = System.currentTimeMillis();
        var newBuild = projectConnection.newBuild();
        var createdBuild = System.currentTimeMillis();
        System.out.printf("Created new Gradle build in %s ms%n", createdBuild - startTime);

        // exclude quarkusGenerateCode, quarkusGenerateDevAppModel, quarkusGenerateCodeDev ->
        // they seem to add substantial runtime.
        String[] cmdArgs = { "classes",
                "-x", "quarkusGenerateCode",
                "-x", "quarkusGenerateDevAppModel",
                "-x", "quarkusGenerateCodeDev" };
        newBuild
                .withArguments(cmdArgs)
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .run();

        var endTime = System.currentTimeMillis();
        System.out.printf("Running 'classes' task took %s ms%n", +endTime - createdBuild);
    }

    // Right now this is a duplicate of io.quarkus.deployment.dev.CompilationProvider.getSourcePath
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
