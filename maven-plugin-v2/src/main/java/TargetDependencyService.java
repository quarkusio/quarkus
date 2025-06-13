import model.*;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.*;

/**
 * Service responsible for calculating target dependencies in Maven projects.
 * Handles phase dependencies, cross-module dependencies, and goal dependencies.
 */
public class TargetDependencyService {
    
    private final Log log;
    private final boolean verbose;

    public TargetDependencyService(Log log, boolean verbose) {
        this.log = log;
        this.verbose = verbose;
    }

    /**
     * Calculate dependencies for a goal target
     */
    public List<String> calculateGoalDependencies(MavenProject project, String executionPhase, 
                                                  String targetName, List<MavenProject> reactorProjects) {
        List<String> dependsOn = new ArrayList<>();
        
        if (executionPhase != null && !executionPhase.isEmpty() && !executionPhase.startsWith("${")) {
            String precedingPhase = getPrecedingPhase(executionPhase);
            if (precedingPhase != null) {
                dependsOn.add(precedingPhase);
            }
            
            List<String> samePhaseDeps = getCrossModuleGoalDependencies(project, executionPhase, 
                                                                        targetName, reactorProjects);
            dependsOn.addAll(samePhaseDeps);
        } else {
            String inferredPhase = inferPhaseFromGoal(extractGoalFromTargetName(targetName));
            if (inferredPhase != null) {
                String precedingPhase = getPrecedingPhase(inferredPhase);
                if (precedingPhase != null) {
                    dependsOn.add(precedingPhase);
                }
                
                List<String> samePhaseDeps = getCrossModuleGoalDependencies(project, inferredPhase, 
                                                                            targetName, reactorProjects);
                dependsOn.addAll(samePhaseDeps);
            }
        }
        
        return dependsOn;
    }

    /**
     * Calculate dependencies for a phase target
     */
    public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets, 
                                                   MavenProject project, List<MavenProject> reactorProjects) {
        List<String> dependsOn = new ArrayList<>();
        
        // Add dependency on preceding phase
        List<String> phaseDependencies = getPhaseDependencies(phase);
        dependsOn.addAll(phaseDependencies);
        
        // Add dependencies on all goals that belong to this phase
        List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
        dependsOn.addAll(goalsForPhase);
        
        // Add cross-module dependencies
        List<String> crossModuleDependencies = getCrossModulePhaseDependencies(project, phase, reactorProjects);
        dependsOn.addAll(crossModuleDependencies);
        
        return dependsOn;
    }

    /**
     * Get phase dependencies (preceding phases)
     */
    public List<String> getPhaseDependencies(String phase) {
        List<String> deps = new ArrayList<>();
        switch (phase) {
            case "test":
                deps.add("compile");
                break;
            case "package":
                deps.add("test");
                break;
            case "verify":
                deps.add("package");
                break;
            case "install":
                deps.add("verify");
                break;
            case "deploy":
                deps.add("install");
                break;
        }
        return deps;
    }

    /**
     * Get all goals that belong to a specific phase
     */
    public List<String> getGoalsForPhase(String phase, Map<String, TargetConfiguration> allTargets) {
        List<String> goalsForPhase = new ArrayList<>();
        
        for (Map.Entry<String, TargetConfiguration> entry : allTargets.entrySet()) {
            String targetName = entry.getKey();
            TargetConfiguration target = entry.getValue();
            
            if (target.getMetadata() != null && 
                "goal".equals(target.getMetadata().getType()) &&
                phase.equals(target.getMetadata().getPhase())) {
                goalsForPhase.add(targetName);
            }
        }
        
        return goalsForPhase;
    }

    /**
     * Get cross-module phase dependencies
     */
    public List<String> getCrossModulePhaseDependencies(MavenProject project, String phase, List<MavenProject> reactorProjects) {
        List<String> crossModuleDeps = new ArrayList<>();
        
        Map<String, MavenProject> artifactToProject = buildReactorProjectMap(reactorProjects);
        
        if (project.getDependencies() != null) {
            for (org.apache.maven.model.Dependency dependency : project.getDependencies()) {
                String depKey = dependency.getGroupId() + ":" + dependency.getArtifactId();
                
                if (artifactToProject.containsKey(depKey)) {
                    String dependentProjectPhase = depKey + ":" + phase;
                    crossModuleDeps.add(dependentProjectPhase);
                    
                    if (verbose) {
                        log.info("Cross-module dependency: " + project.getArtifactId() + ":" + phase + " depends on " + dependentProjectPhase);
                    }
                }
            }
        }
        
        return crossModuleDeps;
    }

    /**
     * Get cross-module goal dependencies
     */
    public List<String> getCrossModuleGoalDependencies(MavenProject project, String phase, String currentTarget, List<MavenProject> reactorProjects) {
        List<String> crossModuleGoalDeps = new ArrayList<>();
        
        if (verbose) {
            log.info("Checking cross-module goal dependencies for " + project.getArtifactId() + ":" + currentTarget + " (phase: " + phase + ")");
        }
        
        Map<String, MavenProject> artifactToProject = buildReactorProjectMap(reactorProjects);
        
        if (project.getDependencies() != null) {
            for (org.apache.maven.model.Dependency dependency : project.getDependencies()) {
                String depKey = dependency.getGroupId() + ":" + dependency.getArtifactId();
                String scope = dependency.getScope();
                
                // Only consider compile and runtime scope dependencies for cross-module goal dependencies
                // Test scope dependencies don't need to be compiled before main compilation
                if (scope != null && ("test".equals(scope) || "provided".equals(scope))) {
                    continue;
                }
                
                if (verbose && artifactToProject.containsKey(depKey)) {
                    log.info("Found reactor dependency: " + depKey + " for " + project.getArtifactId());
                }
                
                if (artifactToProject.containsKey(depKey)) {
                    String dependentGoalTarget = depKey + ":" + currentTarget;
                    crossModuleGoalDeps.add(dependentGoalTarget);
                    
                    if (verbose) {
                        log.info("Cross-module goal dependency: " + project.getArtifactId() + ":" + currentTarget + " depends on " + dependentGoalTarget);
                    }
                }
            }
        }
        
        if (verbose) {
            log.info("Cross-module goal dependencies found: " + crossModuleGoalDeps.size() + " for " + project.getArtifactId() + ":" + currentTarget);
        }
        
        return crossModuleGoalDeps;
    }

    /**
     * Get the preceding phase in the Maven lifecycle
     */
    public String getPrecedingPhase(String phase) {
        if (phase == null || phase.isEmpty()) {
            return null;
        }
        
        String[] allPhases = {
            "pre-clean", "clean", "post-clean",
            "validate", "initialize", "generate-sources", "process-sources",
            "generate-resources", "process-resources", "compile", "process-classes",
            "generate-test-sources", "process-test-sources", "generate-test-resources",
            "process-test-resources", "test-compile", "process-test-classes", "test",
            "prepare-package", "package", "pre-integration-test", "integration-test",
            "post-integration-test", "verify", "install", "deploy",
            "pre-site", "site", "post-site", "site-deploy"
        };
        
        String[] availablePhases = {
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy", "site"
        };
        
        int currentPhaseIndex = -1;
        for (int i = 0; i < allPhases.length; i++) {
            if (allPhases[i].equals(phase)) {
                currentPhaseIndex = i;
                break;
            }
        }
        
        if (currentPhaseIndex == -1) {
            return null;
        }
        
        for (int i = currentPhaseIndex - 1; i >= 0; i--) {
            String candidatePhase = allPhases[i];
            for (String availablePhase : availablePhases) {
                if (availablePhase.equals(candidatePhase)) {
                    return availablePhase;
                }
            }
        }
        
        return null;
    }

    /**
     * Infer the Maven phase from a goal name
     */
    public String inferPhaseFromGoal(String goal) {
        if (goal == null || goal.isEmpty()) {
            return null;
        }
        
        if (goal.equals("compile") || goal.equals("testCompile")) {
            return "compile";
        } else if (goal.equals("test") || goal.contains("test")) {
            return "test";
        } else if (goal.equals("jar") || goal.equals("war") || goal.equals("build") || goal.equals("repackage")) {
            return "package";
        } else if (goal.equals("dev") || goal.equals("run")) {
            return "compile";
        } else if (goal.contains("site") || goal.contains("javadoc")) {
            return "site";
        } else {
            return "compile";
        }
    }

    /**
     * Build a map of reactor projects by artifact key for efficient lookups
     */
    private Map<String, MavenProject> buildReactorProjectMap(List<MavenProject> reactorProjects) {
        Map<String, MavenProject> artifactToProject = new LinkedHashMap<>();
        for (MavenProject reactorProject : reactorProjects) {
            String key = reactorProject.getGroupId() + ":" + reactorProject.getArtifactId();
            artifactToProject.put(key, reactorProject);
        }
        return artifactToProject;
    }

    /**
     * Extract goal name from target name (e.g., "compiler:compile" -> "compile")
     */
    private String extractGoalFromTargetName(String targetName) {
        if (targetName == null || !targetName.contains(":")) {
            return targetName;
        }
        return targetName.substring(targetName.lastIndexOf(":") + 1);
    }
}