package io.quarkus.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

final class ExportUtil {

    private ExportUtil() {
    }

    static void exportToQuarkusDeploymentPath(JavaArchive archive) throws IOException {
        String exportPath = System.getProperty("quarkus.deploymentExportPath");
        if (exportPath == null) {
            return;
        }
        File exportDir = new File(exportPath);
        if (exportDir.exists()) {
            if (!exportDir.isDirectory()) {
                throw new IllegalStateException("Export path is not a directory: " + exportPath);
            }
            try (Stream<Path> stream = Files.walk(exportDir.toPath())) {
                stream.sorted(Comparator.reverseOrder()).map(Path::toFile)
                        .forEach(File::delete);
            }
        } else if (!exportDir.mkdirs()) {
            throw new IllegalStateException("Export path could not be created: " + exportPath);
        }
        File exportFile = new File(exportDir, archive.getName());
        archive.as(ZipExporter.class).exportTo(exportFile);
    }
}
