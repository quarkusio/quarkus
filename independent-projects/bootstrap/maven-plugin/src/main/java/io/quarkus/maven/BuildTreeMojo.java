/**
 *
 */
package io.quarkus.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Displays Quarkus application build dependency tree including the deployment ones.
 */
@Mojo(name = "build-tree", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.NONE)
public class BuildTreeMojo extends AbstractTreeMojo {
}
