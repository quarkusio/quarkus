package io.quarkus.bootstrap.resolver.maven;

import java.io.File;

/**
 * Attempts to resolve POM files from a Gradle local Maven repository cache
 */
public class GradleLocalPomResolver implements LocalPomResolver {

    private static String getRelativeArtifactDir(String groupId, String artifactId) {
        return groupId + File.separator + artifactId;
    }

    private final File repoDir;

    public GradleLocalPomResolver(File localRepoDir) {
        this.repoDir = localRepoDir;
    }

    @Override
    public File resolvePom(String groupId, String artifactId, String version) {
        final File artifactDir = new File(repoDir, getRelativeArtifactDir(groupId, artifactId));
        if (!artifactDir.exists()) {
            return null;
        }
        final String pomFileName = artifactId + "-" + version + ".pom";
        for (File shaDir : artifactDir.listFiles()) {
            if (shaDir.isDirectory()) {
                final File pomFile = new File(shaDir, pomFileName);
                if (pomFile.exists()) {
                    return pomFile;
                }
            }
        }
        return null;
    }
}
