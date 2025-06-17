"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = runExecutor;
exports.batchMavenExecutor = batchMavenExecutor;
const devkit_1 = require("@nx/devkit");
const child_process_1 = require("child_process");
const path_1 = require("path");
const fs_1 = require("fs");
// Regular executor for single task execution
async function runExecutor(options, context) {
    const { goals, projectRoot = '.', verbose = false, mavenPluginPath = 'maven-plugin', timeout = 300000, outputFile, failOnError = true } = options;
    if (!goals || goals.length === 0) {
        const error = 'At least one Maven goal must be specified';
        devkit_1.logger.error(error);
        return { success: false, terminalOutput: error, error };
    }
    // Resolve paths
    const workspaceRoot = context.root;
    const pluginDir = (0, path_1.join)(workspaceRoot, mavenPluginPath);
    const projectDir = (0, path_1.join)(workspaceRoot, projectRoot);
    // Validate plugin directory
    if (!(0, fs_1.existsSync)(pluginDir)) {
        const error = `Maven plugin directory not found: ${pluginDir}`;
        devkit_1.logger.error(error);
        return { success: false, terminalOutput: error, error };
    }
    // Validate project directory
    if (!(0, fs_1.existsSync)(projectDir)) {
        const error = `Project directory not found: ${projectDir}`;
        devkit_1.logger.error(error);
        return { success: false, terminalOutput: error, error };
    }
    // Check for batch executor
    const batchExecutorClasspath = (0, path_1.join)(pluginDir, 'target/classes');
    const dependencyPath = (0, path_1.join)(pluginDir, 'target/dependency');
    if (!(0, fs_1.existsSync)(batchExecutorClasspath)) {
        const error = `Maven plugin not compiled. Run 'mvn compile' in ${pluginDir}`;
        devkit_1.logger.error(error);
        return { success: false, terminalOutput: error, error };
    }
    if (!(0, fs_1.existsSync)(dependencyPath)) {
        const error = `Maven dependencies not copied. Run 'mvn dependency:copy-dependencies' in ${pluginDir}`;
        devkit_1.logger.error(error);
        return { success: false, terminalOutput: error, error };
    }
    try {
        const goalsString = goals.join(',');
        const verboseFlag = verbose ? 'true' : 'false';
        // Build command
        const classpath = `${batchExecutorClasspath}:${dependencyPath}/*`;
        const command = `java -cp "${classpath}" NxMavenBatchExecutor "${goalsString}" "${projectRoot}" ${verboseFlag}`;
        if (verbose) {
            devkit_1.logger.info(`Executing Maven batch command:`);
            devkit_1.logger.info(`  Goals: ${goals.join(', ')}`);
            devkit_1.logger.info(`  Project: ${projectRoot}`);
            devkit_1.logger.info(`  Working directory: ${pluginDir}`);
            devkit_1.logger.info(`  Command: ${command}`);
        }
        // Execute the batch command
        const startTime = Date.now();
        const output = (0, child_process_1.execSync)(command, {
            cwd: pluginDir,
            encoding: 'utf-8',
            timeout: timeout,
            maxBuffer: 10 * 1024 * 1024 // 10MB buffer
        });
        const duration = Date.now() - startTime;
        // Parse JSON output from batch executor
        // The output may contain Maven warnings before the JSON, so find the JSON part
        let result;
        try {
            // Find the JSON output (starts with '{' and ends with '}')
            const lines = output.trim().split('\n');
            let jsonStart = -1;
            let jsonEnd = -1;
            // Find the start of JSON
            for (let i = 0; i < lines.length; i++) {
                if (lines[i].trim().startsWith('{')) {
                    jsonStart = i;
                    break;
                }
            }
            // Find the end of JSON (last '}')
            for (let i = lines.length - 1; i >= 0; i--) {
                if (lines[i].trim().endsWith('}')) {
                    jsonEnd = i;
                    break;
                }
            }
            if (jsonStart === -1 || jsonEnd === -1) {
                throw new Error('No JSON output found');
            }
            const jsonOutput = lines.slice(jsonStart, jsonEnd + 1).join('\n');
            result = JSON.parse(jsonOutput);
        }
        catch (parseError) {
            const error = `Failed to parse batch executor output: ${parseError?.message || parseError}`;
            devkit_1.logger.error(error);
            devkit_1.logger.debug(`Raw output: ${output}`);
            return { success: false, terminalOutput: error, error };
        }
        // Log results
        if (verbose || !result.overallSuccess) {
            devkit_1.logger.info(`Maven batch execution completed in ${duration}ms`);
            devkit_1.logger.info(`Overall success: ${result.overallSuccess}`);
            if (result.errorMessage) {
                devkit_1.logger.error(`Error: ${result.errorMessage}`);
            }
            result.goalResults.forEach((goalResult, index) => {
                const status = goalResult.success ? '✅' : '❌';
                devkit_1.logger.info(`${status} Goal ${index + 1}: ${goalResult.goal} (${goalResult.durationMs}ms)`);
                if (!goalResult.success && goalResult.errors.length > 0) {
                    goalResult.errors.forEach(error => devkit_1.logger.error(`  Error: ${error}`));
                }
                if (verbose && goalResult.output.length > 0) {
                    devkit_1.logger.debug(`  Output: ${goalResult.output.slice(-5).join('\n  ')}`); // Last 5 lines
                }
            });
        }
        // Write output file if specified
        if (outputFile) {
            const outputPath = (0, path_1.join)(workspaceRoot, outputFile);
            (0, fs_1.writeFileSync)(outputPath, JSON.stringify(result, null, 2));
            if (verbose) {
                devkit_1.logger.info(`Results written to: ${outputPath}`);
            }
        }
        // Determine success
        const success = result.overallSuccess || !failOnError;
        if (!success && failOnError) {
            devkit_1.logger.error(`Maven batch execution failed`);
            if (result.errorMessage) {
                devkit_1.logger.error(result.errorMessage);
            }
        }
        return {
            success,
            terminalOutput: result.goalResults.map(r => r.output.join('\n')).join('\n'),
            output: result,
            error: result.errorMessage
        };
    }
    catch (error) {
        const errorMessage = error?.message || String(error);
        devkit_1.logger.error(`Maven batch executor failed: ${errorMessage}`);
        if (verbose) {
            devkit_1.logger.debug(`Error details:`, error);
        }
        return {
            success: false,
            terminalOutput: errorMessage,
            error: errorMessage
        };
    }
}
// Proper Nx batch executor implementation
async function batchMavenExecutor(taskGraph, inputs) {
    const results = {};
    // Group tasks by project to batch goals together
    const tasksByProject = new Map();
    for (const [taskId, options] of Object.entries(inputs)) {
        const task = taskGraph.tasks[taskId];
        const projectName = task.target.project;
        if (!tasksByProject.has(projectName)) {
            tasksByProject.set(projectName, []);
        }
        tasksByProject.get(projectName).push({ taskId, options });
    }
    // Process each project's tasks as a batch
    const projectPromises = Array.from(tasksByProject.entries()).map(async ([projectName, projectTasks]) => {
        try {
            // Collect all goals for this project
            const allGoals = [];
            const taskIds = [];
            let commonOptions = projectTasks[0].options;
            for (const { taskId, options } of projectTasks) {
                allGoals.push(...options.goals);
                taskIds.push(taskId);
                // Use most verbose settings and shortest timeout
                if (options.verbose)
                    commonOptions.verbose = true;
                if (options.timeout && options.timeout < (commonOptions.timeout || 300000)) {
                    commonOptions.timeout = options.timeout;
                }
            }
            // Execute all goals for this project in a single batch
            const batchResult = await executeMavenBatch(allGoals, commonOptions, process.cwd());
            // Distribute results back to individual tasks
            // For simplicity, all tasks in the batch get the same result
            for (const taskId of taskIds) {
                results[taskId] = {
                    success: batchResult.overallSuccess,
                    terminalOutput: batchResult.goalResults.map(r => r.output.join('\n')).join('\n')
                };
            }
        }
        catch (error) {
            // If batch fails, mark all tasks in this project as failed
            for (const { taskId } of projectTasks) {
                results[taskId] = {
                    success: false,
                    terminalOutput: error?.message || String(error)
                };
            }
        }
    });
    // Wait for all project batches to complete
    await Promise.all(projectPromises);
    return results;
}
// Extract the core Maven batch execution logic
async function executeMavenBatch(goals, options, workspaceRoot) {
    const { projectRoot = '.', verbose = false, mavenPluginPath = 'maven-plugin', timeout = 300000 } = options;
    // Resolve paths
    const pluginDir = (0, path_1.join)(workspaceRoot, mavenPluginPath);
    const projectDir = (0, path_1.join)(workspaceRoot, projectRoot);
    // Validate plugin directory
    if (!(0, fs_1.existsSync)(pluginDir)) {
        throw new Error(`Maven plugin directory not found: ${pluginDir}`);
    }
    // Validate project directory
    if (!(0, fs_1.existsSync)(projectDir)) {
        throw new Error(`Project directory not found: ${projectDir}`);
    }
    // Check for batch executor
    const batchExecutorClasspath = (0, path_1.join)(pluginDir, 'target/classes');
    const dependencyPath = (0, path_1.join)(pluginDir, 'target/dependency');
    if (!(0, fs_1.existsSync)(batchExecutorClasspath)) {
        throw new Error(`Maven plugin not compiled. Run 'mvn compile' in ${pluginDir}`);
    }
    if (!(0, fs_1.existsSync)(dependencyPath)) {
        throw new Error(`Maven dependencies not copied. Run 'mvn dependency:copy-dependencies' in ${pluginDir}`);
    }
    const goalsString = goals.join(',');
    const verboseFlag = verbose ? 'true' : 'false';
    // Build command
    const classpath = `${batchExecutorClasspath}:${dependencyPath}/*`;
    const command = `java -cp "${classpath}" NxMavenBatchExecutor "${goalsString}" "${projectRoot}" ${verboseFlag}`;
    if (verbose) {
        devkit_1.logger.info(`Batch executing Maven goals:`);
        devkit_1.logger.info(`  Goals: ${goals.join(', ')}`);
        devkit_1.logger.info(`  Project: ${projectRoot}`);
        devkit_1.logger.info(`  Working directory: ${pluginDir}`);
    }
    // Execute the batch command
    const startTime = Date.now();
    const output = (0, child_process_1.execSync)(command, {
        cwd: pluginDir,
        encoding: 'utf-8',
        timeout: timeout,
        maxBuffer: 10 * 1024 * 1024 // 10MB buffer
    });
    // Parse JSON output from batch executor
    // The output may contain Maven warnings before the JSON, so find the JSON part
    const lines = output.trim().split('\n');
    let jsonStart = -1;
    let jsonEnd = -1;
    // Find the start of JSON
    for (let i = 0; i < lines.length; i++) {
        if (lines[i].trim().startsWith('{')) {
            jsonStart = i;
            break;
        }
    }
    // Find the end of JSON (last '}')
    for (let i = lines.length - 1; i >= 0; i--) {
        if (lines[i].trim().endsWith('}')) {
            jsonEnd = i;
            break;
        }
    }
    if (jsonStart === -1 || jsonEnd === -1) {
        throw new Error('No JSON output found');
    }
    const jsonOutput = lines.slice(jsonStart, jsonEnd + 1).join('\n');
    const result = JSON.parse(jsonOutput);
    if (verbose) {
        devkit_1.logger.info(`Maven batch execution completed`);
        devkit_1.logger.info(`Overall success: ${result.overallSuccess}`);
        if (result.errorMessage) {
            devkit_1.logger.error(`Error: ${result.errorMessage}`);
        }
        result.goalResults.forEach((goalResult, index) => {
            const status = goalResult.success ? '✅' : '❌';
            devkit_1.logger.info(`${status} Goal ${index + 1}: ${goalResult.goal} (${goalResult.durationMs}ms)`);
        });
    }
    return result;
}
