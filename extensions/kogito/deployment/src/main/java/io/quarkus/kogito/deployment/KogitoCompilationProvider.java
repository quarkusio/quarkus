package io.quarkus.kogito.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.modelcompiler.builder.GeneratedFile;
import org.kie.kogito.codegen.ApplicationGenerator;
import org.kie.kogito.codegen.Generator;
import org.kie.kogito.codegen.di.CDIDependencyInjectionAnnotator;

import io.quarkus.dev.JavaCompilationProvider;

public abstract class KogitoCompilationProvider extends JavaCompilationProvider {

    protected static Map<Path, Path> classToSource = new HashMap<>();

    private String appPackageName = System.getProperty("kogito.codegen.packageName", "org.kie.kogito.app");

    @Override
    public Set<String> handledSourcePaths() {
        return Collections.singleton("src" + File.separator + "main" + File.separator + "resources");
    }

    @Override
    public final void compile(Set<File> filesToCompile, Context context) {

        File outputDirectory = context.getOutputDirectory();
        try {

            ApplicationGenerator appGen = new ApplicationGenerator(appPackageName, outputDirectory)
                    .withDependencyInjection(new CDIDependencyInjectionAnnotator());
            Generator generator = addGenerator(appGen, filesToCompile, context);

            Collection<GeneratedFile> generatedFiles = generator.generate();

            HashSet<File> generatedSourceFiles = new HashSet<>();
            for (GeneratedFile file : generatedFiles) {
                Path path = pathOf(outputDirectory.getPath(), file.getPath());
                Files.write(path, file.getData());
                generatedSourceFiles.add(path.toFile());
            }
            super.compile(generatedSourceFiles, context);
        } catch (IOException e) {
            throw new KogitoCompilerException(e);
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, Set<String> sourcePaths, String classesPath) {
        if (classToSource.containsKey(classFilePath)) {
            return classToSource.get(classFilePath);
        }

        return null;
    }

    protected abstract Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException;

    private Path pathOf(String path, String relativePath) {
        Path p = Paths.get(path, relativePath);
        p.getParent().toFile().mkdirs();
        return p;
    }
}
