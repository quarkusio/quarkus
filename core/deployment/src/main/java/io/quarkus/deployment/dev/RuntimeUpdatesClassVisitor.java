package io.quarkus.deployment.dev;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.Gizmo;
import io.quarkus.paths.PathCollection;

public class RuntimeUpdatesClassVisitor extends ClassVisitor {
    private final PathCollection sourcePaths;
    private final String classesPath;
    private String sourceFile;

    public RuntimeUpdatesClassVisitor(PathCollection sourcePaths, String classesPath) {
        super(Gizmo.ASM_API_VERSION);
        this.sourcePaths = sourcePaths;
        this.classesPath = classesPath;
    }

    @Override
    public void visitSource(String source, String debug) {
        this.sourceFile = source;
    }

    public Path getSourceFileForClass(final Path classFilePath) {
        for (Path sourcesDir : sourcePaths) {
            final Path classesDir = Paths.get(classesPath);
            final StringBuilder sourceRelativeDir = new StringBuilder();
            sourceRelativeDir.append(classesDir.relativize(classFilePath.getParent()));
            sourceRelativeDir.append(File.separator);
            sourceRelativeDir.append(sourceFile);
            final Path sourceFilePath = sourcesDir.resolve(Path.of(sourceRelativeDir.toString()));
            if (Files.exists(sourceFilePath)) {
                return sourceFilePath;
            }
        }
        return null;
    }
}
