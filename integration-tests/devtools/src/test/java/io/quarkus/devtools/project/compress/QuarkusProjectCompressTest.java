package io.quarkus.devtools.project.compress;

import static io.quarkus.devtools.project.compress.QuarkusProjectCompress.DIR_UNIX_MODE;
import static io.quarkus.devtools.project.compress.QuarkusProjectCompress.zip;
import static org.apache.commons.io.FileUtils.contentEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.devtools.testing.SnapshotTesting;

class QuarkusProjectCompressTest extends PlatformAwareTestBase {

    @Test
    public void createZip() throws Exception {
        // Given a Quarkus project
        final Path testDir = Paths.get("target/zip");
        SnapshotTesting.deleteTestDirectory(testDir.toFile());
        testDir.toFile().mkdirs();
        Path zip = testDir.resolve("project.zip");

        final Path projectPath = createProject(testDir);

        // When zipping without including the project directory
        zip(projectPath, zip, false);
        assertTrue(zip.toFile().exists());

        // Then the zip content is valid
        final Path unzipProject = testDir.resolve("unzipProject");
        unzip(zip, unzipProject);

        checkUnzipped(projectPath, unzipProject);
    }

    @Test
    public void createZipWithParentFolder() throws Exception {
        // Given a Quarkus project
        final Path testDir = Paths.get("target/zip");
        SnapshotTesting.deleteTestDirectory(testDir.toFile());
        testDir.toFile().mkdirs();
        Path zip = testDir.resolve("project.zip");

        final Path projectPath = createProject(testDir);

        // When zipping without including the project directory
        zip(projectPath, zip, true);
        assertTrue(zip.toFile().exists());

        // Then the zip content is valid and included in a parent directory
        final Path unzipProject = testDir.resolve("unzipProject");
        unzip(zip, unzipProject);

        checkUnzipped(projectPath, unzipProject.resolve(projectPath.getFileName()));
    }

    @Test
    void checkUnixMode() {
        assertEquals(DIR_UNIX_MODE, 040755);
        assertEquals(QuarkusProjectCompress.getFileUnixMode(true), 0100755);
        assertEquals(QuarkusProjectCompress.getFileUnixMode(false), 0100644);
    }

    private void checkUnzipped(Path projectPath, Path unzipProject) throws IOException {
        assertTrue(contentEquals(unzipProject.resolve(".gitignore").toFile(), projectPath.resolve(".gitignore").toFile()));
        assertTrue(contentEquals(unzipProject.resolve("pom.xml").toFile(), projectPath.resolve("pom.xml").toFile()));
    }

    private Path createProject(Path testDir) throws QuarkusCommandException, IOException {
        final Path projectPath = testDir.resolve("project");
        final QuarkusProject project = QuarkusProjectHelper.getProject(projectPath, BuildTool.MAVEN);
        final QuarkusCommandOutcome result = new CreateProject(project)
                .groupId("org.acme")
                .artifactId("basic-rest")
                .version("1.0.0-SNAPSHOT")
                .execute();
        // Create a fake wrapper
        Files.write(projectPath.resolve("mvnw"), "testmvnw".getBytes());
        projectPath.resolve("mvnw").toFile().setExecutable(true);
        Files.write(projectPath.resolve("mvnw.bat"), "testmvnw".getBytes());
        projectPath.resolve("mvnw.bat").toFile().setExecutable(true);
        assertTrue(result.isSuccess());

        return projectPath;
    }

    private static void unzip(final Path zipPath, final Path outputDir) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipPath.toFile()); ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[1024];
            while (zipEntry != null) {
                File newFile = createEntryFile(outputDir.toFile(), zipEntry);
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    private static File createEntryFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

}
