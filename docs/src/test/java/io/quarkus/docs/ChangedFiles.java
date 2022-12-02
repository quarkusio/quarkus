package io.quarkus.docs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class ChangedFiles implements AutoCloseable {

    public static Path getPath(String propertyName, String defaultValue) {
        String pathValue = System.getProperty(propertyName);
        if (pathValue != null) {
            return Path.of(pathValue);
        }
        return Path.of("").resolve(defaultValue);
    }

    final Repository repo;
    final Git git;

    public ChangedFiles(Path gitDir) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repo = builder.setGitDir(gitDir.toFile()).setMustExist(true).build();
        git = new Git(repo);
    }

    public Collection<String> modifiedFiles(Path subtree, Function<String, String> transform) throws GitAPIException {
        List<DiffEntry> diff = git.diff().call();
        Set<String> paths = new HashSet<>();
        for (DiffEntry e : diff) {
            String newPath = e.getNewPath();
            if (!newPath.equals("/dev/null") && newPath.startsWith(subtree.toString())) {
                // If file was not deleted..
                paths.add(transform.apply(newPath));
            }
        }
        return paths;
    }

    @Override
    public void close() throws Exception {
        git.close();
        repo.close();
    }
}
