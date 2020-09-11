/**
 *
 */
package io.quarkus.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Displays Quarkus application build dependency tree including the deployment ones.
 * 
 * @deprecated this mojo has moved to the quarkus-maven-plugin
 */
@Mojo(name = "build-tree", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.NONE)
@Deprecated
public class BuildTreeMojo extends AbstractTreeMojo {
}
