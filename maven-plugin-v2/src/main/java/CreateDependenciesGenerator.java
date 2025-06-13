import model.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import java.io.File;
import java.util.*;

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
            String source = NxPathUtils.getProjectName(project);
            String sourceFile = NxPathUtils.getRelativePomPath(project, workspaceRoot);

            log.info("Dependency analysis progress: " + (i + 1) + "/" + projects.size() + " projects");

            // 1. Static dependencies - direct Maven dependencies
            int prevStatic = staticDeps;
            staticDeps += addStaticDependencies(dependencies, project, artifactToProject, sourceFile);

            // 2. Implicit dependencies - parent/module relationships
            int prevImplicit = implicitDeps;
            implicitDeps += addImplicitDependencies(dependencies, project, projects, source);

            if (verbose && log != null && projects.size() <= 20) {
                int newStatic = staticDeps - prevStatic;
                int newImplicit = implicitDeps - prevImplicit;
                if (newStatic > 0 || newImplicit > 0) {
                    log.info("Project " + source + ": " + newStatic + " static, " + newImplicit + " implicit dependencies");
                }
            }
        }

        if (verbose && log != null) {
            log.info("Dependency analysis complete: " + staticDeps + " static, " + implicitDeps + " implicit dependencies");
        }

        return dependencies;
    }

    /**
     * Build mapping from artifactId to project name for workspace projects
     */
    private static Map<String, String> buildArtifactMapping(List<MavenProject> projects) {
        Map<String, String> mapping = new HashMap<>();
        for (MavenProject project : projects) {
            String projectName = NxPathUtils.getProjectName(project);
            if (project.getGroupId() != null && project.getArtifactId() != null) {
                String key = project.getGroupId() + ":" + project.getArtifactId();
                mapping.put(key, projectName);
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
        String source = NxPathUtils.getProjectName(project);
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

    /**
     * Add implicit dependencies (parent-child module relationships)
     * Returns: number of dependencies added
     */
    private static int addImplicitDependencies(List<RawProjectGraphDependency> dependencies,
                                                MavenProject project,
                                                List<MavenProject> allProjects,
                                                String source) {
        // Find parent-child relationships
        File projectDir = project.getBasedir();
        int count = 0;

        for (MavenProject otherProject : allProjects) {
            File otherDir = otherProject.getBasedir();
            String target = NxPathUtils.getProjectName(otherProject);

            if (!source.equals(target)) {
                // Check if one is parent of the other
                if (NxPathUtils.isParentChildRelation(projectDir, otherDir)) {
                    RawProjectGraphDependency dependency = new RawProjectGraphDependency(
                        source, target, RawProjectGraphDependency.DependencyType.IMPLICIT);
                    dependencies.add(dependency);
                    count++;
                }
            }
        }

        return count;
    }


    /**
     * Check if a dependency is a test-scoped dependency
     */
    private static boolean isTestDependency(Dependency dep) {
        return "test".equals(dep.getScope());
    }

    /**
     * Check if a dependency is optional
     */
    private static boolean isOptionalDependency(Dependency dep) {
        return dep.isOptional();
    }
}
