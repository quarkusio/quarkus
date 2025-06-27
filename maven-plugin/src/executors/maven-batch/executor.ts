import { ExecutorContext, logger, TaskGraph } from '@nx/devkit';
import { execSync } from 'child_process';
import { join } from 'path';
import { existsSync, writeFileSync } from 'fs';

export interface MavenBatchExecutorOptions {
  goals: string[];
  projectRoot?: string;
  verbose?: boolean;
  mavenPluginPath?: string;
  outputFile?: string;
  failOnError?: boolean;
}

export interface MavenGoalResult {
  goal: string;
  success: boolean;
  durationMs: number;
  exitCode: number;
  output: string[];
  errors: string[];
}

export interface MavenBatchResult {
  overallSuccess: boolean;
  totalDurationMs: number;
  errorMessage?: string;
  goalResults: MavenGoalResult[];
}

export interface ExecutorResult {
  success: boolean;
  terminalOutput: string;
  output?: MavenBatchResult;
  error?: string;
}

// Regular executor for single task execution
export default async function runExecutor(
  options: MavenBatchExecutorOptions,
  context: ExecutorContext
): Promise<ExecutorResult> {
  const {
    goals,
    projectRoot = '.',
    verbose = false,
    mavenPluginPath = 'maven-plugin',
    outputFile,
    failOnError = true
  } = options;

  if (!goals || goals.length === 0) {
    const error = 'At least one Maven goal must be specified';
    logger.error(error);
    return { success: false, terminalOutput: error, error };
  }

  // Resolve paths
  const workspaceRoot = context.root;
  const pluginDir = join(workspaceRoot, mavenPluginPath);
  const projectDir = join(workspaceRoot, projectRoot);

  // Validate plugin directory
  if (!existsSync(pluginDir)) {
    const error = `Maven plugin directory not found: ${pluginDir}`;
    logger.error(error);
    return { success: false, terminalOutput: error, error };
  }

  // Validate project directory
  if (!existsSync(projectDir)) {
    const error = `Project directory not found: ${projectDir}`;
    logger.error(error);
    return { success: false, terminalOutput: error, error };
  }

  // Check for batch executor
  const batchExecutorClasspath = join(pluginDir, 'target/classes');
  const dependencyPath = join(pluginDir, 'target/dependency');
  
  if (!existsSync(batchExecutorClasspath)) {
    const error = `Maven plugin not compiled. Run 'mvn compile' in ${pluginDir}`;
    logger.error(error);
    return { success: false, terminalOutput: error, error };
  }

  if (!existsSync(dependencyPath)) {
    const error = `Maven dependencies not copied. Run 'mvn dependency:copy-dependencies' in ${pluginDir}`;
    logger.error(error);
    return { success: false, terminalOutput: error, error };
  }

  try {
    const goalsString = goals.join(',');
    const verboseFlag = verbose ? 'true' : 'false';
    
    // Build command with new signature: goals, workspaceRoot, projects, verbose
    const classpath = `${batchExecutorClasspath}:${dependencyPath}/*`;
    const command = `java -Dmaven.multiModuleProjectDirectory="${workspaceRoot}" -cp "${classpath}" NxMavenBatchExecutor "${goalsString}" "${workspaceRoot}" "${projectRoot}" ${verboseFlag}`;

    if (verbose) {
      logger.info(`Executing Maven batch command:`);
      logger.info(`  Goals: ${goals.join(', ')}`);
      logger.info(`  Project: ${projectRoot}`);
      logger.info(`  Working directory: ${pluginDir}`);
      logger.info(`  Command: ${command}`);
    }

    // Execute the batch command
    const startTime = Date.now();
    const output = execSync(command, {
      cwd: pluginDir,
      encoding: 'utf-8',
      maxBuffer: 10 * 1024 * 1024 // 10MB buffer
    });

    const duration = Date.now() - startTime;
    
    // Parse JSON output from batch executor
    // The output may contain Maven warnings before the JSON, so find the JSON part
    let result: MavenBatchResult;
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
    } catch (parseError: any) {
      const error = `Failed to parse batch executor output: ${parseError?.message || parseError}`;
      logger.error(error);
      logger.debug(`Raw output: ${output}`);
      return { success: false, terminalOutput: error, error };
    }

    // Log results
    if (verbose || !result.overallSuccess) {
      logger.info(`Maven batch execution completed in ${duration}ms`);
      logger.info(`Overall success: ${result.overallSuccess}`);
      
      if (result.errorMessage) {
        logger.error(`Error: ${result.errorMessage}`);
      }

      result.goalResults.forEach((goalResult, index) => {
        const status = goalResult.success ? '✅' : '❌';
        logger.info(`${status} Goal ${index + 1}: ${goalResult.goal} (${goalResult.durationMs}ms)`);
        
        if (!goalResult.success && goalResult.errors.length > 0) {
          goalResult.errors.forEach(error => logger.error(`  Error: ${error}`));
        }
        
        if (verbose && goalResult.output.length > 0) {
          logger.debug(`  Output: ${goalResult.output.slice(-5).join('\n  ')}`); // Last 5 lines
        }
      });
    }

    // Write output file if specified
    if (outputFile) {
      const outputPath = join(workspaceRoot, outputFile);
      writeFileSync(outputPath, JSON.stringify(result, null, 2));
      if (verbose) {
        logger.info(`Results written to: ${outputPath}`);
      }
    }

    // Determine success
    const success = result.overallSuccess || !failOnError;
    
    if (!success && failOnError) {
      logger.error(`Maven batch execution failed`);
      if (result.errorMessage) {
        logger.error(result.errorMessage);
      }
    }

    return {
      success,
      terminalOutput: result.goalResults.map(r => r.output.join('\n')).join('\n'),
      output: result,
      error: result.errorMessage
    };

  } catch (error: any) {
    const errorMessage = error?.message || String(error);
    logger.error(`Maven batch executor failed: ${errorMessage}`);
    
    if (verbose) {
      logger.debug(`Error details:`, error);
    }

    return {
      success: false,
      terminalOutput: errorMessage,
      error: errorMessage
    };
  }
}

// Simplified Nx batch executor - collect all goals and projects from task graph and execute in one batch
export async function batchMavenExecutor(
  taskGraph: TaskGraph,
  inputs: Record<string, MavenBatchExecutorOptions>
): Promise<Record<string, { success: boolean; terminalOutput: string }>> {
  const results: Record<string, { success: boolean; terminalOutput: string }> = {};
  
  try {
    // Collect ALL goals and projects from ALL tasks in the task graph
    const allGoals: string[] = [];
    const allProjects: string[] = [];
    const taskIds: string[] = [];
    let verbose = false;
    let commonOptions: MavenBatchExecutorOptions | undefined;
    
    // Extract goals and project roots from each task in the task graph
    for (const [taskId, options] of Object.entries(inputs)) {
      const task = taskGraph.tasks[taskId];
      
      if (task) {
        // Get goals from the task's configuration options
        const taskGoals = options.goals || [];
        allGoals.push(...taskGoals);
        
        // Get project root (use from options or task target project)
        const projectRoot = options.projectRoot || task.target.project || '.';
        allProjects.push(projectRoot);
        
        taskIds.push(taskId);
        
        // Use first task's options as base, enable verbose if any task requests it
        if (!commonOptions) commonOptions = options;
        if (options.verbose) verbose = true;
      }
    }
    
    // Remove duplicate goals and projects
    const uniqueGoals = Array.from(new Set(allGoals));
    const uniqueProjects = Array.from(new Set(allProjects));
    
    // If no tasks or goals, return empty results
    if (taskIds.length === 0 || uniqueGoals.length === 0) {
      return results;
    }
    
    // Execute ALL unique goals across ALL unique projects in a single batch
    const batchOptions = { ...commonOptions!, verbose };
    const batchResult = await executeMultiProjectMavenBatch(uniqueGoals, uniqueProjects, batchOptions, process.cwd());
    
    // All tasks get the same result (success/failure of the entire batch)
    for (const taskId of taskIds) {
      results[taskId] = {
        success: batchResult.overallSuccess,
        terminalOutput: batchResult.goalResults.map(r => r.output.join('\n')).join('\n')
      };
    }
    
    return results;
    
  } catch (error: any) {
    // If batch fails, mark ALL tasks as failed
    for (const taskId of Object.keys(inputs)) {
      results[taskId] = {
        success: false,
        terminalOutput: error?.message || String(error)
      };
    }
  }
  
  return results;
}

// Execute Maven goals across multiple projects in a single batch
async function executeMultiProjectMavenBatch(
  goals: string[],
  projects: string[],
  options: MavenBatchExecutorOptions,
  workspaceRoot: string
): Promise<MavenBatchResult> {
  const {
    verbose = false,
    mavenPluginPath = 'maven-plugin'
  } = options;

  // Resolve paths
  const pluginDir = join(workspaceRoot, mavenPluginPath);

  // Validate plugin directory
  if (!existsSync(pluginDir)) {
    throw new Error(`Maven plugin directory not found: ${pluginDir}`);
  }

  // Check for batch executor
  const batchExecutorClasspath = join(pluginDir, 'target/classes');
  const dependencyPath = join(pluginDir, 'target/dependency');
  
  if (!existsSync(batchExecutorClasspath)) {
    throw new Error(`Maven plugin not compiled. Run 'mvn compile' in ${pluginDir}`);
  }

  if (!existsSync(dependencyPath)) {
    throw new Error(`Maven dependencies not copied. Run 'mvn dependency:copy-dependencies' in ${pluginDir}`);
  }

  const goalsString = goals.join(',');
  const projectsString = projects.join(',');
  const verboseFlag = verbose ? 'true' : 'false';
  
  // Build command with new signature: goals, workspaceRoot, projects, verbose
  const classpath = `${batchExecutorClasspath}:${dependencyPath}/*`;
  const command = `java -Dmaven.multiModuleProjectDirectory="${workspaceRoot}" -cp "${classpath}" NxMavenBatchExecutor "${goalsString}" "${workspaceRoot}" "${projectsString}" ${verboseFlag}`;

  // Execute the batch command
  const startTime = Date.now();
  const output = execSync(command, {
    cwd: pluginDir,
    encoding: 'utf-8',
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
  const result: MavenBatchResult = JSON.parse(jsonOutput);
  
  if (verbose) {
    logger.info(`Multi-project Maven batch execution completed`);
    logger.info(`Overall success: ${result.overallSuccess}`);
    
    if (result.errorMessage) {
      logger.error(`Error: ${result.errorMessage}`);
    }

    result.goalResults.forEach((goalResult, index) => {
      const status = goalResult.success ? '✅' : '❌';
      logger.info(`${status} Goal ${index + 1}: ${goalResult.goal} (${goalResult.durationMs}ms)`);
    });
  }
  
  return result;
}