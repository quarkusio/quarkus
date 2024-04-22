package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.util.Objects;

public class MavenLocalPomResolver implements LocalPomResolver {

    private static String getRelativePomPath(String groupId, String artifactId, String version) {
        return groupId.replace('.', File.separatorChar) + File.separator +
                artifactId + File.separator +
                version + File.separator +
                artifactId + '-' +
                version + ".pom";
    }

    private final File repoDir;

    public MavenLocalPomResolver(File repoDir) {
        this.repoDir = Objects.requireNonNull(repoDir);
    }

    @Override
    public File resolvePom(String groupId, String artifactId, String version) {
        var pom = new File(repoDir, getRelativePomPath(groupId, artifactId, version));
        return pom.exists() ? pom : null;
    }
}
