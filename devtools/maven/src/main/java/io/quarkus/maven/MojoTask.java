package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;

@FunctionalInterface
public interface MojoTask {
    void run() throws MojoExecutionException;
}
