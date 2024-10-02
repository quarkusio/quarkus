package io.quarkus.test;

import static io.quarkus.test.ExportUtil.APPLICATION_PROPERTIES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

final class ExportUtil {

    static final String APPLICATION_PROPERTIES = "application.properties";

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

    static void mergeCustomApplicationProperties(JavaArchive archive, Properties customApplicationProperties)
            throws IOException {
        Node applicationProperties = archive.get(APPLICATION_PROPERTIES);
        if (applicationProperties != null) {
            // Merge the existing "application.properties" asset and overriden config properties
            // Overriden properties take precedence
            Properties mergedProperties = new Properties();
            Asset asset = applicationProperties.getAsset();
            if (asset instanceof StringAsset strAsset) {
                mergedProperties.load(new StringReader(strAsset.getSource()));
            } else {
                try (InputStream in = asset.openStream()) {
                    mergedProperties.load(in);
                }
            }
            customApplicationProperties.forEach(mergedProperties::put);

            if (Boolean.parseBoolean(System.getProperty("quarkus.test.log-merged-properties"))) {
                System.out.println("Merged config properties:\n"
                        + mergedProperties.keySet().stream().map(Object::toString).collect(Collectors.joining("\n")));
            } else {
                System.out.println(
                        "NOTE: overrideConfigKey() and application.properties were merged; use quarkus.test.log-merged-properties=true to list the specific values");
            }
            deleteApplicationProperties(archive);
            archive.add(new PropertiesAsset(mergedProperties), APPLICATION_PROPERTIES);
        } else {
            archive.add(new PropertiesAsset(customApplicationProperties), APPLICATION_PROPERTIES);
        }
    }

    static void deleteApplicationProperties(JavaArchive archive) {
        // MemoryMapArchiveBase#addAsset(ArchivePath,Asset) does not overwrite the existing node correctly
        // https://github.com/shrinkwrap/shrinkwrap/issues/179
        archive.delete(APPLICATION_PROPERTIES);
    }
}
