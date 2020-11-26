package io.quarkus.devtools.commands;

import static io.quarkus.devtools.ProjectTestUtil.checkContains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;

public class CreateJBangProjectTest extends PlatformAwareTestBase {
    @Test
    public void createRESTEasy() throws Exception {
        final File file = new File("target/jbang-resteasy");
        final Path projectDir = file.toPath();
        ProjectTestUtil.delete(file);
        assertCreateJBangProject(newCreateJBangProject(projectDir)
                .setValue("noJBangWrapper", false));

        assertThat(projectDir.resolve("jbang")).exists();

        assertThat(projectDir.resolve("src/GreetingResource.java"))
                .exists()
                .satisfies(checkContains("//usr/bin/env jbang \"$0\" \"$@\" ; exit $?"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy"));
    }

    @Test
    public void createRESTEasyWithNoJBangWrapper() throws Exception {
        final File file = new File("target/jbang-resteasy");
        final Path projectDir = file.toPath();
        ProjectTestUtil.delete(file);
        assertCreateJBangProject(newCreateJBangProject(projectDir)
                .setValue("noJBangWrapper", true));

        assertThat(projectDir.resolve("jbang")).doesNotExist();

        assertThat(projectDir.resolve("src/GreetingResource.java"))
                .exists()
                .satisfies(checkContains("//usr/bin/env jbang \"$0\" \"$@\" ; exit $?"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy"));
    }

    @Test
    public void createRESTEasyWithExtensions() throws Exception {
        final File file = new File("target/jbang-resteasy");
        final Path projectDir = file.toPath();
        ProjectTestUtil.delete(file);
        Set<String> extensions = new HashSet<>();
        extensions.add("resteasy-jsonb");

        assertCreateJBangProject(newCreateJBangProject(projectDir)
                .extensions(extensions)
                .setValue("noJBangWrapper", false));

        assertThat(projectDir.resolve("jbang")).exists();

        assertThat(projectDir.resolve("src/GreetingResource.java"))
                .exists()
                .satisfies(checkContains("//usr/bin/env jbang \"$0\" \"$@\" ; exit $?"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy"))
                .satisfies(checkContains("//DEPS io.quarkus:quarkus-resteasy-jsonb"));
    }

    private CreateJBangProject newCreateJBangProject(Path dir) {
        return new CreateJBangProject(dir, getPlatformDescriptor());
    }

    private void assertCreateJBangProject(CreateJBangProject createJBangProjectProject)
            throws QuarkusCommandException {
        final QuarkusCommandOutcome result = createJBangProjectProject
                .execute();
        assertTrue(result.isSuccess());
    }
}
