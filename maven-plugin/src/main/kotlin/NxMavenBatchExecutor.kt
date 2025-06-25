import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.apache.maven.shared.invoker.*
import java.io.File
import kotlin.system.exitProcess

/**
 * Nx Maven Batch Executor - Executes multiple Maven goals in a single session
 * while maintaining proper artifact context and providing detailed per-goal results.
 */
object NxMavenBatchExecutor {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 3) {
            System.err.println("Usage: java NxMavenBatchExecutor <goals> <workspaceRoot> <projects> [verbose]")
            System.err.println("Example: java NxMavenBatchExecutor \"maven-jar-plugin:jar,maven-install-plugin:install\" \"/workspace\" \".,module1,module2\" true")
            exitProcess(1)
        }

        val goalsList = args[0]
        val workspaceRoot = args[1]
        val projectsList = args[2]
        val verbose = args.size > 3 && args[3].toBoolean()

        try {
            val goals = goalsList.split(",")
            val projects = projectsList.split(",")
            val result = executeBatch(goals, workspaceRoot, projects, verbose)
            
            // Output JSON result for Nx to parse
            println(gson.toJson(result))
            
            // Exit with appropriate code
            exitProcess(if (result.isOverallSuccess()) 0 else 1)
            
        } catch (e: Exception) {
            val errorResult = BatchExecutionResult().apply {
                setOverallSuccess(false)
                setErrorMessage("Batch executor failed: ${e.message}")
            }
            
            System.err.println(gson.toJson(errorResult))
            exitProcess(1)
        }
    }

    /**
     * Execute multiple Maven goals across multiple projects in a single session
     */
    fun executeBatch(goals: List<String>, workspaceRoot: String, projects: List<String>, verbose: Boolean): BatchExecutionResult {
        val batchResult = BatchExecutionResult().apply {
            setOverallSuccess(true)
        }
        
        try {
            val workspaceDir = File(workspaceRoot)
            val rootPomFile = File(workspaceDir, "pom.xml")
            
            if (!rootPomFile.exists()) {
                throw RuntimeException("Root pom.xml not found in workspace: $workspaceRoot")
            }

            val batchStartTime = System.currentTimeMillis()

            // Execute all goals across all projects in single Maven invoker session
            val batchGoalResult = executeMultiProjectGoalsWithInvoker(goals, projects, workspaceDir, rootPomFile, verbose)
            batchResult.addGoalResult(batchGoalResult)
            batchResult.setOverallSuccess(batchGoalResult.isSuccess())
            if (!batchGoalResult.isSuccess()) {
                batchResult.setErrorMessage("Multi-project batch goal execution failed")
            }
            
            val batchDuration = System.currentTimeMillis() - batchStartTime
            batchResult.setTotalDurationMs(batchDuration)
            
        } catch (e: Exception) {
            batchResult.setOverallSuccess(false)
            batchResult.setErrorMessage("Batch execution failed: ${e.message}")
        }
        
        return batchResult
    }

    /**
     * Execute multiple goals across multiple projects using Maven Invoker API
     */
    private fun executeMultiProjectGoalsWithInvoker(
        goals: List<String>, 
        projects: List<String>, 
        workspaceDir: File, 
        rootPomFile: File, 
        verbose: Boolean
    ): GoalExecutionResult {
        val goalResult = GoalExecutionResult().apply {
            setGoal("${goals.joinToString(",")} (across ${projects.size} projects)")
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Capture output
            val outputLines = mutableListOf<String>()
            val errorLines = mutableListOf<String>()
            
            val invoker = DefaultInvoker()
            
            // Find Maven executable - check MAVEN_HOME first, then try to find mvn in PATH
            val mavenHome = System.getenv("MAVEN_HOME")
            if (mavenHome != null) {
                invoker.mavenHome = File(mavenHome)
            } else {
                // Try to find Maven executable in PATH
                val mavenExecutable = findMavenExecutable()
                if (mavenExecutable != null) {
                    // Set Maven home to parent directory of mvn executable
                    val mvnFile = File(mavenExecutable)
                    val binDir = mvnFile.parentFile
                    if (binDir != null && binDir.name == "bin") {
                        invoker.mavenHome = binDir.parentFile
                    }
                }
            }
            
            val request = DefaultInvocationRequest().apply {
                pomFile = rootPomFile
                baseDirectory = workspaceDir
                setGoals(goals) // Execute all goals in single Maven session
            }
            
            // Use Maven's -pl option to specify which projects to build
            // Convert project paths to Maven module identifiers
            val projectList = projects.map { project ->
                if ("." == project) {
                    // Root project - use the artifact ID from root pom
                    "."
                } else {
                    // Child module - use relative path
                    project
                }
            }
            
            if (projectList.isNotEmpty() && projectList != listOf(".")) {
                // Only use -pl if we're not building everything (i.e., not just root)
                val projectsArg = projectList.joinToString(",")
                request.projects = projectsArg.split(",")
            }
            
            // Set output handlers
            request.setOutputHandler { line ->
                outputLines.add(line)
                if (verbose) {
                    println("[MULTI-PROJECT] $line")
                }
            }
            
            request.setErrorHandler { line ->
                errorLines.add(line)
                if (verbose) {
                    System.err.println("[MULTI-PROJECT ERROR] $line")
                }
            }

            if (verbose) {
                println("Executing goals: ${goals.joinToString(", ")}")
                println("Across projects: ${projects.joinToString(", ")}")
                println("Working directory: ${workspaceDir.absolutePath}")
            }

            // Execute all goals across all projects in single Maven reactor session
            val result = invoker.execute(request)
            
            val duration = System.currentTimeMillis() - startTime
            
            goalResult.apply {
                setSuccess(result.exitCode == 0)
                setDurationMs(duration)
                setOutput(outputLines)
                setErrors(errorLines)
                setExitCode(result.exitCode)
            }
            
            if (result.executionException != null) {
                goalResult.setErrorMessage(result.executionException.message)
            }
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            goalResult.apply {
                setSuccess(false)
                setDurationMs(duration)
                setErrorMessage("Multi-project goal execution exception: ${e.message}")
                setErrors(listOf(e.message ?: "Unknown error"))
            }
        }
        
        return goalResult
    }

    /**
     * Find Maven executable in PATH
     */
    private fun findMavenExecutable(): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val pathSeparator = System.getProperty("path.separator")
        val paths = pathEnv.split(pathSeparator)
        
        val mvnCommand = if (System.getProperty("os.name").lowercase().contains("windows")) "mvn.cmd" else "mvn"
        
        for (path in paths) {
            val mvnFile = File(path, mvnCommand)
            if (mvnFile.exists() && mvnFile.canExecute()) {
                return mvnFile.absolutePath
            }
        }
        
        return null
    }

    /**
     * Result of executing a batch of Maven goals
     */
    class BatchExecutionResult {
        @SerializedName("overallSuccess")
        private var _overallSuccess: Boolean = false
        
        @SerializedName("totalDurationMs")
        private var _totalDurationMs: Long = 0
        
        @SerializedName("errorMessage")
        private var _errorMessage: String? = null
        
        @SerializedName("goalResults")
        private var _goalResults: MutableList<GoalExecutionResult> = mutableListOf()
        
        fun addGoalResult(goalResult: GoalExecutionResult) {
            _goalResults.add(goalResult)
        }
        
        // Java-compatible getters and setters
        fun isOverallSuccess() = _overallSuccess
        fun setOverallSuccess(success: Boolean) { _overallSuccess = success }
        fun getTotalDurationMs() = _totalDurationMs
        fun setTotalDurationMs(duration: Long) { _totalDurationMs = duration }
        fun getErrorMessage() = _errorMessage
        fun setErrorMessage(message: String?) { _errorMessage = message }
        fun getGoalResults() = _goalResults
        fun setGoalResults(results: MutableList<GoalExecutionResult>) { _goalResults = results }
    }

    /**
     * Result of executing a single Maven goal
     */
    class GoalExecutionResult {
        @SerializedName("goal")
        private var _goal: String? = null
        
        @SerializedName("success")
        private var _success: Boolean = false
        
        @SerializedName("durationMs")
        private var _durationMs: Long = 0
        
        @SerializedName("exitCode")
        private var _exitCode: Int = 0
        
        @SerializedName("errorMessage")
        private var _errorMessage: String? = null
        
        @SerializedName("output")
        private var _output: List<String> = mutableListOf()
        
        @SerializedName("errors")
        private var _errors: List<String> = mutableListOf()
        
        // Java-compatible getters and setters
        fun getGoal() = _goal
        fun setGoal(goalName: String?) { _goal = goalName }
        fun isSuccess() = _success
        fun setSuccess(isSuccessful: Boolean) { _success = isSuccessful }
        fun getDurationMs() = _durationMs
        fun setDurationMs(duration: Long) { _durationMs = duration }
        fun getExitCode() = _exitCode
        fun setExitCode(code: Int) { _exitCode = code }
        fun getErrorMessage() = _errorMessage
        fun setErrorMessage(message: String?) { _errorMessage = message }
        fun getOutput() = _output
        fun setOutput(outputLines: List<String>) { _output = outputLines }
        fun getErrors() = _errors
        fun setErrors(errorLines: List<String>) { _errors = errorLines }
    }
}