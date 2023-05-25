package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.File;

import org.eclipse.aether.artifact.DefaultArtifact;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;

public class Playground {

    public static void main(String[] args) throws Exception {

        MavenArtifactResolver.builder()
                .setWorkspaceDiscovery(false)
                .setUserSettings(new File("/home/aloubyansky/playground/quarkus3-proxy-auth-issue/settings.xml"))
                .build()
                .resolve(new DefaultArtifact("org.mvnpm", "lit", "pom", "2.7.4"));
    }
}
