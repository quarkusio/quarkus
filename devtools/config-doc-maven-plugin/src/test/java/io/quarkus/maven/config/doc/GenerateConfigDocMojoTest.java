package io.quarkus.maven.config.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateConfigDocMojoTest {

    @Test
    void findsTargetDirectoryOfARealModule(@TempDir Path root) throws IOException, MojoExecutionException {
        Path moduleTarget = createModuleWithTarget(root, "module-a");

        List<Path> found = GenerateConfigDocMojo.findTargetDirectories(root);

        assertThat(found).containsExactly(moduleTarget);
    }

    @Test
    void scanRootBeingAGitCheckoutDoesNotHideItsOwnModules(@TempDir Path root)
            throws IOException, MojoExecutionException {
        // the scan root is normally the project's own repo root, which of course has a .git directory;
        // that must not cause the whole scan to be treated as "nested" and skipped
        Files.createDirectories(root.resolve(".git"));
        Path moduleTarget = createModuleWithTarget(root, "module-a");

        List<Path> found = GenerateConfigDocMojo.findTargetDirectories(root);

        assertThat(found).containsExactly(moduleTarget);
    }

    @Test
    void ignoresTargetDirectoriesInsideNestedGitCheckouts(@TempDir Path root)
            throws IOException, MojoExecutionException {
        Path realModuleTarget = createModuleWithTarget(root, "module-a");

        // e.g. a git worktree for another branch, kept inside the project directory,
        // that still has stale build output from a previous build
        Path nestedCheckout = root.resolve(".claude").resolve("worktrees").resolve("stale-branch");
        Files.createDirectories(nestedCheckout.resolve(".git"));
        createModuleWithTarget(nestedCheckout, "module-a");

        List<Path> found = GenerateConfigDocMojo.findTargetDirectories(root);

        assertThat(found).containsExactly(realModuleTarget);
    }

    @Test
    void ignoresNestedGitCheckoutMarkedByAGitFile(@TempDir Path root) throws IOException, MojoExecutionException {
        // git worktrees use a `.git` *file* (not a directory) pointing at the real git dir
        Path realModuleTarget = createModuleWithTarget(root, "module-a");

        Path worktree = root.resolve(".claude").resolve("worktrees").resolve("issue-1234");
        Files.createDirectories(worktree);
        Files.writeString(worktree.resolve(".git"), "gitdir: /somewhere/else\n");
        createModuleWithTarget(worktree, "module-a");

        List<Path> found = GenerateConfigDocMojo.findTargetDirectories(root);

        assertThat(found).containsExactly(realModuleTarget);
    }

    @Test
    void ignoresTargetDirectoriesDeeplyNestedInsideAGitCheckout(@TempDir Path root)
            throws IOException, MojoExecutionException {
        // mirrors the real-world case: the .git marker is several directories above the
        // polluting target/ dir, e.g. .claude/worktrees/<branch>/model-providers/<ext>/runtime/target
        Path realModuleTarget = createModuleWithTarget(root, "module-a");

        Path worktreeRoot = root.resolve(".claude").resolve("worktrees").resolve("issue-2221");
        Files.createDirectories(worktreeRoot.resolve(".git"));
        Path deeplyNestedModuleParent = worktreeRoot.resolve("model-providers").resolve("openai")
                .resolve("openai-vanilla");
        createModuleWithTarget(deeplyNestedModuleParent, "runtime");

        List<Path> found = GenerateConfigDocMojo.findTargetDirectories(root);

        assertThat(found).containsExactly(realModuleTarget);
    }

    private static Path createModuleWithTarget(Path parent, String moduleName) throws IOException {
        Path moduleDir = parent.resolve(moduleName);
        Path target = moduleDir.resolve("target");
        Files.createDirectories(target);
        Files.createFile(moduleDir.resolve("pom.xml"));
        return target;
    }
}
