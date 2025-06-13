import model.TargetConfiguration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for calculating target dependencies in Maven projects.
 * Handles phase dependencies, cross-module dependencies, and goal dependencies.
 */
public class TargetDependencyService {
    
    private final Log log;
    private final boolean verbose;
    private final MavenSession session;

    public TargetDependencyService(Log log, boolean verbose, MavenSession session) {
        this.log = log;
        this.verbose = verbose;
        this.session = session;
    }

    /**
     * Calculate dependencies for a goal target
     */
    public List<String> calculateGoalDependencies(MavenProject project, String executionPhase, 
                                                  String targetName, List<MavenProject> reactorProjects) {
        List<String> dependsOn = new ArrayList<>();
        
        
        String effectivePhase = executionPhase;
        if (effectivePhase == null || effectivePhase.isEmpty() || effectivePhase.startsWith("${")) {
            effectivePhase = inferPhaseFromGoal(MavenUtils.extractGoalFromTargetName(targetName));
        }
        
        // Fallback: map common goals to known phases if inference fails
        if (effectivePhase == null || effectivePhase.isEmpty()) {
            String goal = MavenUtils.extractGoalFromTargetName(targetName);
            effectivePhase = mapGoalToPhase(goal);
        }
        
        if (effectivePhase != null && !effectivePhase.isEmpty()) {
            // Add dependency on preceding phase (but don't fail if this doesn't work)
            try {
                String precedingPhase = getPrecedingPhase(effectivePhase);
                if (precedingPhase != null && !precedingPhase.isEmpty()) {
                    dependsOn.add(precedingPhase);
                }
            } catch (Exception e) {
                // Continue even if preceding phase detection fails
            }
            
            // ALWAYS add cross-module dependencies using Nx ^ syntax
            // Goals depend on their phase across all dependent projects
            dependsOn.add("^" + effectivePhase);
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
        
        // Add cross-module dependencies using Nx ^ syntax
        dependsOn.add("^" + phase);
        
        return dependsOn;
    }

    /**
     * Get phase dependencies (preceding phases)
     */
    public List<String> getPhaseDependencies(String phase) {
        List<String> deps = new ArrayList<>();
        String precedingPhase = getPrecedingPhase(phase);
        if (precedingPhase != null) {
            deps.add(precedingPhase);
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
     * Get Maven execution plan using the lifecycle executor
     */
    private MavenExecutionPlan getExecutionPlan() {
        try {
            LifecycleExecutor lifecycleExecutor = session.getContainer().lookup(LifecycleExecutor.class);
            List<String> goals = session.getGoals();
            return lifecycleExecutor.calculateExecutionPlan(session, goals.toArray(new String[0]));
        } catch (Exception e) {
            if (verbose) {
                log.warn("Could not access Maven execution plan: " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Get the preceding phase in the Maven lifecycle
     */
    public String getPrecedingPhase(String phase) {
        if (phase == null || phase.isEmpty()) {
            return null;
        }
        
        MavenExecutionPlan executionPlan = getExecutionPlan();
        if (executionPlan == null) {
            return null;
        }
        
        List<String> allPhases = new ArrayList<>();
        Set<String> seenPhases = new LinkedHashSet<>();
        
        for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
            String executionPhase = mojoExecution.getLifecyclePhase();
            if (executionPhase != null && !executionPhase.isEmpty()) {
                seenPhases.add(executionPhase);
            }
        }
        allPhases.addAll(seenPhases);
        
        int currentPhaseIndex = allPhases.indexOf(phase);
        if (currentPhaseIndex > 0) {
            return allPhases.get(currentPhaseIndex - 1);
        }
        
        return null;
    }

    /**
     * Simple mapping of common goals to their typical phases
     */
    private String mapGoalToPhase(String goal) {
        if (goal == null) return null;
        
        switch (goal) {
            case "clean": return "clean";
            case "compile": return "compile";
            case "testCompile": return "test-compile";
            case "test": return "test";
            case "package": return "package";
            case "jar": return "package";
            case "war": return "package";
            case "install": return "install";
            case "deploy": return "deploy";
            case "site": return "site";
            default: return null;
        }
    }

    /**
     * Infer the Maven phase from a goal name by examining plugin configurations
     */
    public String inferPhaseFromGoal(String goal) {
        if (goal == null || goal.isEmpty()) {
            return null;
        }
        
        MavenExecutionPlan executionPlan = getExecutionPlan();
        if (executionPlan == null) {
            return null;
        }
        
        for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
            String executionGoal = mojoExecution.getGoal();
            if (goal.equals(executionGoal) || goal.endsWith(":" + executionGoal)) {
                return mojoExecution.getLifecyclePhase();
            }
        }
        
        return null;
    }

    
    /**
     * Get Nx project path from Maven project (relative path from workspace root)
     */
    private String getProjectPath(MavenProject project) {
        if (session != null) {
            File workspaceRoot = new File(session.getExecutionRootDirectory());
            String relativePath = NxPathUtils.getRelativePath(workspaceRoot, project.getBasedir());
            return relativePath.isEmpty() ? "." : relativePath;
        } else {
            // Fallback to project base directory name
            return project.getBasedir().getName();
        }
    }

}