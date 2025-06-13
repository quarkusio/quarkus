import model.RawProjectGraphDependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates CreateDependencies compatible results for Nx integration
 */
public class CreateDependenciesGenerator {

    /**
     * Generate CreateDependencies results from Maven project list
     * Returns: List<RawProjectGraphDependency>
     */
    public static List<RawProjectGraphDependency> generateCreateDependencies(List<MavenProject> projects, File workspaceRoot) {
        return generateCreateDependencies(projects, workspaceRoot, null, false);
    }

    /**
     * Generate CreateDependencies results with logging support
     */
    public static List<RawProjectGraphDependency> generateCreateDependencies(List<MavenProject> projects, File workspaceRoot, Log log, boolean verbose) {
        List<RawProjectGraphDependency> dependencies = new ArrayList<>();

        // Extract workspace projects
        Map<String, String> artifactToProject = buildArtifactMapping(projects);

        if (verbose && log != null) {
            log.info("Built artifact mapping for " + artifactToProject.size() + " workspace projects");
        }

        int staticDeps = 0;
        int implicitDeps = 0;

        for (int i = 0; i < projects.size(); i++) {
            MavenProject project = projects.get(i);
            String source = project.getGroupId() + ":" + project.getArtifactId();
            String sourceFile = NxPathUtils.getRelativePomPath(project, workspaceRoot);

            // Show progress every 100 projects to reduce log spam
            if (i % 100 == 0 || i == projects.size() - 1) {
                log.info("Dependency analysis progress: " + (i + 1) + "/" + projects.size() + " projects");
            }

            // 1. Static dependencies - direct Maven dependencies
            int prevStatic = staticDeps;
            staticDeps += addStaticDependencies(dependencies, project, artifactToProject, sourceFile);

            // 2. Skip implicit dependencies for better performance
            // They're rarely needed and cause O(n²) complexity
            // int prevImplicit = implicitDeps;
            // implicitDeps += addImplicitDependencies(dependencies, project, projects, source);

            if (verbose && log != null && projects.size() <= 20) {
                int newStatic = staticDeps - prevStatic;
                if (newStatic > 0) {
                    log.info("Project " + source + ": " + newStatic + " static dependencies");
                }
            }
        }

        if (verbose && log != null) {
            log.info("Dependency analysis complete: " + staticDeps + " static dependencies");
        }

        return dependencies;
    }

    /**
     * Build mapping from artifactId to project name for workspace projects
     */
    private static Map<String, String> buildArtifactMapping(List<MavenProject> projects) {
        Map<String, String> mapping = new HashMap<>();
        for (MavenProject project : projects) {
            if (project.getGroupId() != null && project.getArtifactId() != null) {
                String key = MavenUtils.formatProjectKey(project);
                String projectName = MavenUtils.formatProjectKey(project);
                mapping.put(key, projectName);
            } else {
                System.err.println("Warning: Skipping project with null groupId or artifactId: " + project);
            }
        }
        return mapping;
    }

    /**
     * Add static dependencies (explicit Maven dependencies between workspace projects)
     * Returns: number of dependencies added
     */
    private static int addStaticDependencies(List<RawProjectGraphDependency> dependencies,
                                              MavenProject project,
                                              Map<String, String> artifactToProject,
                                              String sourceFile) {
        String source = project.getGroupId() + ":" + project.getArtifactId();
        int count = 0;

        // Check both declared dependencies and resolved artifacts
        if (project.getDependencies() != null) {
            for (Dependency dep : project.getDependencies()) {
                if (dep.getGroupId() != null && dep.getArtifactId() != null) {
                    String depKey = dep.getGroupId() + ":" + dep.getArtifactId();

                    // Check if this dependency refers to another project in workspace
                    String target = artifactToProject.get(depKey);
                    if (target != null && !target.equals(source)) {
                        RawProjectGraphDependency dependency = new RawProjectGraphDependency(
                            source, target, RawProjectGraphDependency.DependencyType.STATIC, sourceFile);
                        dependencies.add(dependency);
                        count++;
                    }
                }
            }
        }

        return count;
    }



}
