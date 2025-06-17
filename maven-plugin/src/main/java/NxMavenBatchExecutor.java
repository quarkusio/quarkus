import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.*;

/**
 * Nx Maven Batch Executor - Executes multiple Maven goals in a single session
 * while maintaining proper artifact context and providing detailed per-goal results.
 */
public class NxMavenBatchExecutor {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java NxMavenBatchExecutor <goals> <workspaceRoot> <projects> [verbose]");
            System.err.println("Example: java NxMavenBatchExecutor \"maven-jar-plugin:jar,maven-install-plugin:install\" \"/workspace\" \".,module1,module2\" true");
            System.exit(1);
        }

        String goalsList = args[0];
        String workspaceRoot = args[1];
        String projectsList = args[2];
        boolean verbose = args.length > 3 && Boolean.parseBoolean(args[3]);

        try {
            List<String> goals = Arrays.asList(goalsList.split(","));
            List<String> projects = Arrays.asList(projectsList.split(","));
            BatchExecutionResult result = executeBatch(goals, workspaceRoot, projects, verbose);
            
            // Output JSON result for Nx to parse
            System.out.println(gson.toJson(result));
            
            // Exit with appropriate code
            System.exit(result.isOverallSuccess() ? 0 : 1);
            
        } catch (Exception e) {
            BatchExecutionResult errorResult = new BatchExecutionResult();
            errorResult.setOverallSuccess(false);
            errorResult.setErrorMessage("Batch executor failed: " + e.getMessage());
            
            System.err.println(gson.toJson(errorResult));
            System.exit(1);
        }
    }

    /**
     * Execute multiple Maven goals across multiple projects in a single session
     */
    public static BatchExecutionResult executeBatch(List<String> goals, String workspaceRoot, List<String> projects, boolean verbose) {
        BatchExecutionResult batchResult = new BatchExecutionResult();
        batchResult.setOverallSuccess(true);
        
        try {
            File workspaceDir = new File(workspaceRoot);
            File rootPomFile = new File(workspaceDir, "pom.xml");
            
            if (!rootPomFile.exists()) {
                throw new RuntimeException("Root pom.xml not found in workspace: " + workspaceRoot);
            }

            long batchStartTime = System.currentTimeMillis();

            // Execute all goals across all projects in single Maven invoker session
            GoalExecutionResult batchGoalResult = executeMultiProjectGoalsWithInvoker(goals, projects, workspaceDir, rootPomFile, verbose);
            batchResult.addGoalResult(batchGoalResult);
            batchResult.setOverallSuccess(batchGoalResult.isSuccess());
            if (!batchGoalResult.isSuccess()) {
                batchResult.setErrorMessage("Multi-project batch goal execution failed");
            }
            
            long batchDuration = System.currentTimeMillis() - batchStartTime;
            batchResult.setTotalDurationMs(batchDuration);
            
        } catch (Exception e) {
            batchResult.setOverallSuccess(false);
            batchResult.setErrorMessage("Batch execution failed: " + e.getMessage());
        }
        
        return batchResult;
    }

    /**
     * Execute multiple goals across multiple projects using Maven Invoker API
     */
    private static GoalExecutionResult executeMultiProjectGoalsWithInvoker(List<String> goals, List<String> projects, File workspaceDir, File rootPomFile, boolean verbose) {
        GoalExecutionResult goalResult = new GoalExecutionResult();
        goalResult.setGoal(String.join(",", goals) + " (across " + projects.size() + " projects)");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Capture output
            List<String> outputLines = new ArrayList<>();
            List<String> errorLines = new ArrayList<>();
            
            Invoker invoker = new DefaultInvoker();
            
            // Find Maven executable
            String mavenHome = System.getenv("MAVEN_HOME");
            if (mavenHome != null) {
                invoker.setMavenHome(new File(mavenHome));
            }
            
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(rootPomFile);
            request.setBaseDirectory(workspaceDir);
            request.setGoals(goals); // Execute all goals in single Maven session
            
            // Use Maven's -pl option to specify which projects to build
            // Convert project paths to Maven module identifiers
            List<String> projectList = new ArrayList<>();
            for (String project : projects) {
                if (".".equals(project)) {
                    // Root project - use the artifact ID from root pom
                    projectList.add(".");
                } else {
                    // Child module - use relative path
                    projectList.add(project);
                }
            }
            
            if (!projectList.isEmpty() && !projectList.equals(Arrays.asList("."))) {
                // Only use -pl if we're not building everything (i.e., not just root)
                String projectsArg = String.join(",", projectList);
                request.setProjects(Arrays.asList(projectsArg.split(",")));
            }
            
            // Set output handlers
            request.setOutputHandler(line -> {
                outputLines.add(line);
                if (verbose) {
                    System.out.println("[MULTI-PROJECT] " + line);
                }
            });
            
            request.setErrorHandler(line -> {
                errorLines.add(line);
                if (verbose) {
                    System.err.println("[MULTI-PROJECT ERROR] " + line);
                }
            });

            if (verbose) {
                System.out.println("Executing goals: " + String.join(", ", goals));
                System.out.println("Across projects: " + String.join(", ", projects));
                System.out.println("Working directory: " + workspaceDir.getAbsolutePath());
            }

            // Execute all goals across all projects in single Maven reactor session
            InvocationResult result = invoker.execute(request);
            
            long duration = System.currentTimeMillis() - startTime;
            
            goalResult.setSuccess(result.getExitCode() == 0);
            goalResult.setDurationMs(duration);
            goalResult.setOutput(outputLines);
            goalResult.setErrors(errorLines);
            goalResult.setExitCode(result.getExitCode());
            
            if (result.getExecutionException() != null) {
                goalResult.setErrorMessage(result.getExecutionException().getMessage());
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            goalResult.setSuccess(false);
            goalResult.setDurationMs(duration);
            goalResult.setErrorMessage("Multi-project goal execution exception: " + e.getMessage());
            goalResult.setErrors(Arrays.asList(e.getMessage()));
        }
        
        return goalResult;
    }


    /**
     * Result of executing a batch of Maven goals
     */
    public static class BatchExecutionResult {
        private boolean overallSuccess;
        private long totalDurationMs;
        private String errorMessage;
        private List<GoalExecutionResult> goalResults = new ArrayList<>();

        public boolean isOverallSuccess() { return overallSuccess; }
        public void setOverallSuccess(boolean overallSuccess) { this.overallSuccess = overallSuccess; }
        
        public long getTotalDurationMs() { return totalDurationMs; }
        public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<GoalExecutionResult> getGoalResults() { return goalResults; }
        public void setGoalResults(List<GoalExecutionResult> goalResults) { this.goalResults = goalResults; }
        
        public void addGoalResult(GoalExecutionResult goalResult) {
            this.goalResults.add(goalResult);
        }
    }

    /**
     * Result of executing a single Maven goal
     */
    public static class GoalExecutionResult {
        private String goal;
        private boolean success;
        private long durationMs;
        private int exitCode;
        private String errorMessage;
        private List<String> output = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public String getGoal() { return goal; }
        public void setGoal(String goal) { this.goal = goal; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<String> getOutput() { return output; }
        public void setOutput(List<String> output) { this.output = output; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
}