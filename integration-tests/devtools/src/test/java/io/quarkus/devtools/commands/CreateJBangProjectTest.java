package io.quarkus.devtools.commands;

import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.devtools.testing.SnapshotTesting;

public class CreateJBangProjectTest extends PlatformAwareTestBase {
    @Test
    public void createRESTEasy() throws Exception {
        final File file = new File("target/jbang-resteasy");
        final Path projectDir = file.toPath();
        SnapshotTesting.deleteTestDirectory(file);
        assertCreateJBangProject(newCreateJBangProject(projectDir)
                .setValue("noJBangWrapper", false));

        assertThat(projectDir.resolve("jbang")).exists();

        assertThat(projectDir.resolve("src/main.java"))
                .exists()
                .satisfies(checkContains("//usr/bin/env jbang \"$0\" \"$@\" ; exit $?"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy"));
    }

    @Test
    public void createRESTEasyWithNoJBangWrapper() throws Exception {
        final File file = new File("target/jbang-resteasy");
        final Path projectDir = file.toPath();
        SnapshotTesting.deleteTestDirectory(file);
        assertCreateJBangProject(newCreateJBangProject(projectDir)
                .setValue("noJBangWrapper", true));

        assertThat(projectDir.resolve("jbang")).doesNotExist();

        assertThat(projectDir.resolve("src/main.java"))
                .exists()
                .satisfies(checkContains("//usr/bin/env jbang \"$0\" \"$@\" ; exit $?"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy"));
    }

    @Test
    public void createRESTEasyWithExtensions() throws Exception {
        final File file = new File("target/jbang-resteasy");
        final Path projectDir = file.toPath();
        SnapshotTesting.deleteTestDirectory(file);
        Set<String> extensions = new HashSet<>();
        extensions.add("resteasy-jsonb");

        assertCreateJBangProject(newCreateJBangProject(projectDir)
                .extensions(extensions)
                .setValue("noJBangWrapper", false));

        assertThat(projectDir.resolve("jbang")).exists();

        assertThat(projectDir.resolve("src/main.java"))
                .exists()
                .satisfies(checkContains("//usr/bin/env jbang \"$0\" \"$@\" ; exit $?"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy-jsonb"));
    }

    private CreateJBangProject newCreateJBangProject(Path dir) {
        return new CreateJBangProject(QuarkusProjectHelper.getProject(dir, BuildTool.MAVEN));
    }

    private void assertCreateJBangProject(CreateJBangProject createJBangProjectProject)
            throws QuarkusCommandException {
        final QuarkusCommandOutcome result = createJBangProjectProject.execute();
        assertTrue(result.isSuccess());
    }
}
