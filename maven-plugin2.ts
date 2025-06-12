import {
  CreateDependencies,
  CreateNodesV2,
  CreateNodesContextV2,
  CreateNodesResult,
  DependencyType,
  detectPackageManager,
  NxJsonConfiguration,
  TargetConfiguration,
  workspaceRoot,
} from '@nx/devkit';
import { dirname, join, relative } from 'path';
import { existsSync, readFileSync, unlinkSync } from 'fs';
import { exec, execSync, spawn } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);


export interface MavenPluginOptions {
  buildTargetName?: string;
  testTargetName?: string;
  serveTargetName?: string;
  javaExecutable?: string;
  mavenExecutable?: string;
  compilerArgs?: string[];
}

const DEFAULT_OPTIONS: MavenPluginOptions = {
  buildTargetName: 'build',
  testTargetName: 'test',
  serveTargetName: 'serve',
  javaExecutable: 'java',
  mavenExecutable: 'mvn',
  compilerArgs: [],
};

/**
 * Maven plugin that uses a Java program to analyze pom.xml files
 * and generate Nx project configurations with comprehensive dependencies
 */
export const createNodesV2: CreateNodesV2 = [
  '**/pom.xml',
  async (configFiles, options, context) => {
    const opts: MavenPluginOptions = Object.assign({}, DEFAULT_OPTIONS, options || {});

    console.log(`Maven plugin found ${configFiles.length} total pom.xml files`);

    // Filter out the maven-script pom.xml and build artifact directories  
    const filteredConfigFiles = configFiles.filter(file =>
      !file.includes('maven-script/pom.xml') &&
      !file.includes('target/') &&
      !file.includes('node_modules/')
    );

    // No limits - process all projects but in efficient batches

    console.log(`Filtered config files: ${filteredConfigFiles.length} files`);

    if (filteredConfigFiles.length === 0) {
      return [];
    }

    try {
      // Send all files to Java - let Java handle batching and memory management
      const batchResults = await generateBatchNxConfigFromMavenAsync(filteredConfigFiles, opts, context);

      console.log(`Generated ${Object.keys(batchResults).length} Maven project configurations`);

      // Convert batch results to the expected format
      const tupleResults: Array<[string, any]> = [];
      
      for (const [projectRoot, nxConfig] of Object.entries(batchResults)) {
        // Convert absolute path from Java back to relative path for matching
        const relativePath = projectRoot.startsWith(workspaceRoot) 
          ? projectRoot.substring(workspaceRoot.length + 1)
          : projectRoot;
          
        // Find the corresponding config file
        const configFile = filteredConfigFiles.find(file => dirname(file) === relativePath);
        if (!configFile) {
          console.log(`DEBUG: No config file found for projectRoot: "${relativePath}" (from: "${projectRoot}")`);
          if (Object.keys(batchResults).length < 5) {
            console.log(`DEBUG: Available config files: ${filteredConfigFiles.slice(0, 3).join(', ')}`);
          }
          continue;
        }


        // Normalize and validate targets
        const normalizedTargets = normalizeTargets(nxConfig.targets || {}, nxConfig, opts);

        const result = {
          projects: {
            [relativePath]: {
              name: nxConfig.name || projectRoot.split('/').pop() || 'unknown',
              root: relativePath,
              sourceRoot: nxConfig.sourceRoot || join(relativePath, 'src/main/java'),
              projectType: nxConfig.projectType || 'library',
              targets: normalizedTargets,
              tags: nxConfig.tags || [],
              implicitDependencies: (nxConfig.implicitDependencies?.projects || []).concat(nxConfig.implicitDependencies?.inheritsFrom || []),
              namedInputs: nxConfig.namedInputs,
              metadata: {
                technologies: ['maven', 'java'],
                framework: detectFramework(nxConfig),
              },
            },
          },
        };

        tupleResults.push([configFile, result]);
      }
      
      console.log(`Generated ${tupleResults.length} valid project configurations`);
      
      // Debug: log first few project names
      if (tupleResults.length > 0) {
        console.log(`DEBUG: First 3 projects: ${tupleResults.slice(0, 3).map(([file, result]) => 
          Object.keys(result.projects)[0] + ' -> ' + (Object.values(result.projects)[0] as any).name
        ).join(', ')}`);
      }
      
      return tupleResults;

    } catch (error) {
      console.error(`Failed to process Maven projects with Java analyzer:`, error.message);
      throw new Error(`Maven analysis failed: ${error.message}`);
    }
  },
];

/**
 * Create dependencies based on Maven dependency analysis
 */

/**
 * Create dependencies (no caching needed - Java handles batch processing)
 */
export const createDependencies: CreateDependencies = async () => {
  console.log(`DEBUG: Dependency creation called`);
  
  // Dependencies are handled via target dependsOn configuration
  return [];
};

/**
 * Generate Nx configuration by calling the Java Maven analyzer (async version)
 */
async function generateNxConfigFromMavenAsync(
  pomPath: string,
  options: MavenPluginOptions,
  context: CreateNodesContextV2
): Promise<any> {
  const javaAnalyzerPath = findJavaAnalyzer();
  if (!javaAnalyzerPath) {
    throw new Error('Maven analyzer Java program not found. Please ensure maven-script is compiled.');
  }

  try {
    // Use Maven exec to run the Java program with proper classpath
    const mavenScriptDir = javaAnalyzerPath.endsWith('.jar')
      ? dirname(javaAnalyzerPath)
      : dirname(dirname(javaAnalyzerPath)); // target/classes -> project root

    // Convert relative path to absolute path
    const absolutePomPath = pomPath.startsWith('/') ? pomPath : join(workspaceRoot, pomPath);
    
    console.log(`Analyzing ${absolutePomPath} using Maven script in ${mavenScriptDir}`);

    const command = `${options.mavenExecutable} exec:java -Dexec.mainClass="MavenModelReader" -Dexec.args="${absolutePomPath} --nx" -q`;
    const { stdout, stderr } = await execAsync(command, {
      cwd: mavenScriptDir,
      timeout: 30000, // 30 second timeout
      maxBuffer: 1024 * 1024 * 10, // 10MB buffer for large outputs
    });

    if (stderr && stderr.trim()) {
      console.warn(`Maven analyzer stderr: ${stderr}`);
    }

    // Parse JSON output from the Java program
    const output = stdout;
    const jsonStart = output.indexOf('{');
    const jsonEnd = output.lastIndexOf('}') + 1;

    if (jsonStart === -1 || jsonEnd === 0) {
      throw new Error('No valid JSON output from Maven analyzer');
    }

    const jsonOutput = output.substring(jsonStart, jsonEnd);
    return JSON.parse(jsonOutput);

  } catch (error) {
    throw new Error(`Failed to execute Maven analyzer: ${error.message}`);
  }
}

// Track active Maven processes for cleanup
const activeMavenProcesses = new Set<any>();

// Cleanup function for Maven processes
function cleanupMavenProcesses(): void {
  console.log(`[Maven Plugin2] Cleaning up ${activeMavenProcesses.size} active Maven processes`);
  for (const process of activeMavenProcesses) {
    try {
      if (process && process.pid && !process.killed) {
        console.log(`[Maven Plugin2] Killing Maven process ${process.pid}`);
        process.kill('SIGTERM');
        setTimeout(() => {
          if (process.pid && !process.killed) {
            console.log(`[Maven Plugin2] Force killing Maven process ${process.pid}`);
            process.kill('SIGKILL');
          }
        }, 2000);
      }
    } catch (error: any) {
      console.warn(`[Maven Plugin2] Error cleaning up Maven process:`, error?.message || error);
    }
  }
  activeMavenProcesses.clear();
}

// Ensure cleanup on process exit
process.on('exit', () => {
  cleanupMavenProcesses();
});

process.on('SIGINT', () => {
  console.log('\n[Maven Plugin2] Received SIGINT, cleaning up Maven processes...');
  cleanupMavenProcesses();
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\n[Maven Plugin2] Received SIGTERM, cleaning up Maven processes...');
  cleanupMavenProcesses();
  process.exit(0);
});

process.on('uncaughtException', (error) => {
  console.error('[Maven Plugin2] Uncaught exception, cleaning up Maven processes...', error);
  cleanupMavenProcesses();
  process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('[Maven Plugin2] Unhandled rejection, cleaning up Maven processes...', reason);
  cleanupMavenProcesses();
});

/**
 * Generate Nx configurations for multiple projects using batch processing
 */
async function generateBatchNxConfigFromMavenAsync(
  pomPaths: string[],
  options: MavenPluginOptions,
  context: CreateNodesContextV2
): Promise<Record<string, any>> {
  const javaAnalyzerPath = findJavaAnalyzer();
  if (!javaAnalyzerPath) {
    throw new Error('Maven analyzer Java program not found. Please ensure maven-script is compiled.');
  }
  
  try {
    // Use Maven exec to run the Java program with stdin
    const mavenScriptDir = javaAnalyzerPath.endsWith('.jar') 
      ? dirname(javaAnalyzerPath) 
      : dirname(dirname(javaAnalyzerPath)); // target/classes -> project root
    
    // Convert relative paths to absolute paths
    const absolutePomPaths = pomPaths.map(pomPath => 
      pomPath.startsWith('/') ? pomPath : join(workspaceRoot, pomPath)
    );
    
    console.log(`Processing ${absolutePomPaths.length} Maven projects...`);
    
    // Use consistent output file path
    const outputFile = join(workspaceRoot, 'maven-script/maven-results.json');
    
    const { stdout, stderr } = await new Promise<{stdout: string, stderr: string}>((resolve, reject) => {
      const child = spawn(options.mavenExecutable, [
        'exec:java',
        '-Dexec.mainClass=MavenModelReader',
        '-Dexec.args=--hierarchical --nx',
        `-Dmaven.output.file=${outputFile}`,
        `-Duser.dir=${workspaceRoot}`,
        '-q'
      ], {
        cwd: mavenScriptDir,
        stdio: ['pipe', 'pipe', 'pipe']
      });

      // Track this process for cleanup
      activeMavenProcesses.add(child);

      let stdout = '';
      let stderr = '';

      child.stdout?.on('data', (data) => {
        stdout += data.toString();
      });

      child.stderr?.on('data', (data) => {
        const text = data.toString();
        stderr += text;
        // Only show important logs, not debug spam
        if (text.includes('ERROR:') || text.includes('Final results')) {
          console.log(`[Java] ${text.trim()}`);
        }
      });

      child.on('close', (code) => {
        // Remove from active processes when done
        activeMavenProcesses.delete(child);
        
        if (code === 0) {
          resolve({ stdout, stderr });
        } else {
          reject(new Error(`Maven process exited with code ${code}. stderr: ${stderr}`));
        }
      });

      child.on('error', (error) => {
        // Remove from active processes on error
        activeMavenProcesses.delete(child);
        reject(new Error(`Failed to spawn Maven process: ${error.message}`));
      });

      // No stdin input needed for hierarchical traversal
      child.stdin?.end();

      // Shorter timeout for responsiveness with proper cleanup
      const timeoutId = setTimeout(() => {
        if (activeMavenProcesses.has(child)) {
          console.warn(`[Maven Plugin2] Maven process timed out, killing process ${child.pid}`);
          try {
            child.kill('SIGTERM');
            setTimeout(() => {
              if (child.pid && !child.killed) {
                child.kill('SIGKILL');
              }
            }, 2000);
          } catch (error: any) {
            console.warn(`[Maven Plugin2] Error killing timed out process:`, error?.message);
          }
          activeMavenProcesses.delete(child);
        }
        reject(new Error('Maven process timed out after 1 minute'));
      }, 60000);

      // Clear timeout if process completes
      child.on('close', () => clearTimeout(timeoutId));
    });
    
    if (stderr && stderr.trim()) {
      console.warn(`Maven analyzer stderr: ${stderr}`);
    }
    
    // Check if process completed successfully
    if (!stdout.includes('SUCCESS:')) {
      throw new Error('Maven analyzer did not complete successfully');
    }
    
    // Read JSON output from the file
    try {
      const jsonContent = readFileSync(outputFile, 'utf8');
      
      // Clean up the temp file
      try {
        unlinkSync(outputFile);
      } catch (e) {
        console.warn(`Could not delete temp file ${outputFile}: ${e.message}`);
      }
      
      const result = JSON.parse(jsonContent);
      
      // Check for errors in the result
      if (result._errors && result._errors.length > 0) {
        console.warn(`Maven analyzer encountered ${result._errors.length} errors:`);
        result._errors.forEach((error: string, index: number) => {
          console.warn(`  ${index + 1}. ${error}`);
        });
      }
      
      // Log statistics
      if (result._stats) {
        console.log(`Maven analysis stats: ${result._stats.successful}/${result._stats.processed} projects processed successfully (${result._stats.errors} errors)`);
      }
      
      // Remove metadata fields from result before returning
      delete result._errors;
      delete result._stats;
      
      return result;
    } catch (error) {
      throw new Error(`Failed to read output file ${outputFile}: ${error.message}`);
    }
    
  } catch (error) {
    throw new Error(`Failed to execute Maven analyzer in batch mode: ${error.message}`);
  }
}

/**
 * Build classpath including Maven dependencies
 */
function buildClasspath(javaAnalyzerPath: string): string {
  const classpathParts: string[] = [javaAnalyzerPath];

  // If it's a JAR file, just return it
  if (javaAnalyzerPath.endsWith('.jar')) {
    return javaAnalyzerPath;
  }

  // For compiled classes, we need to include Maven dependencies
  const mavenScriptDir = dirname(javaAnalyzerPath); // target/classes -> target
  const projectDir = dirname(mavenScriptDir);       // target -> project root

  try {
    // Use Maven to build dependency classpath
    const command = 'mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout';
    const dependencyClasspath = execSync(command, {
      cwd: projectDir,
      encoding: 'utf8',
      timeout: 30000,
    }).trim();

    if (dependencyClasspath) {
      classpathParts.push(dependencyClasspath);
    }
  } catch (error) {
    console.warn('Failed to build Maven dependency classpath, using basic classpath');
  }

  return classpathParts.join(process.platform === 'win32' ? ';' : ':');
}

/**
 * Find the compiled Java Maven analyzer
 */
function findJavaAnalyzer(): string | null {
  // Look for the compiled Java program in common locations
  const possiblePaths = [
    join(workspaceRoot, 'maven-script/target/classes'),
    join(workspaceRoot, 'tools/maven-script/target/classes'),
    join(workspaceRoot, 'scripts/maven-script/target/classes'),
    join(__dirname, '../maven-script/target/classes'),
  ];

  for (const path of possiblePaths) {
    if (existsSync(join(path, 'MavenModelReader.class'))) {
      return path;
    }
  }

  // Also check if there's a JAR file
  const jarPaths = [
    join(workspaceRoot, 'maven-script/target/maven-script-1.0-SNAPSHOT.jar'),
    join(workspaceRoot, 'tools/maven-script/target/maven-script-1.0-SNAPSHOT.jar'),
  ];

  for (const jarPath of jarPaths) {
    if (existsSync(jarPath)) {
      return jarPath;
    }
  }

  return null;
}

// Simplified function for finding Maven projects
function findMavenProjects(): string[] {
  try {
    const command = 'find . -name "pom.xml" -type f';
    const output = execSync(command, {
      cwd: workspaceRoot,
      encoding: 'utf8',
    });

    const pomFiles = output.trim().split('\n').filter(Boolean);
    return pomFiles.map(pomFile => dirname(pomFile.replace('./', '')))
                   .filter(path => path !== '.');
  } catch {
    return [];
  }
}

// Simplified function for getting project name
function getProjectName(projectPath: string): string | null {
  return projectPath.split('/').pop() || null;
}

// Find a project by its Maven artifactId
function findProjectByName(artifactId: string, mavenProjects: string[]): string | null {
  // Try to find by directory name first (most common case)
  for (const projectPath of mavenProjects) {
    const dirName = projectPath.split('/').pop();
    if (dirName === artifactId) {
      return dirName;
    }
  }

  // Try to find by parsing pom.xml artifactId for more accurate matching
  for (const projectPath of mavenProjects) {
    try {
      const pomPath = join(workspaceRoot, projectPath, 'pom.xml');
      if (existsSync(pomPath)) {
        const pomContent = readFileSync(pomPath, 'utf8');
        const artifactIdMatch = pomContent.match(/<artifactId>([^<]+)<\/artifactId>/);
        if (artifactIdMatch && artifactIdMatch[1] === artifactId) {
          return projectPath.split('/').pop() || artifactId;
        }
      }
    } catch {
      // Ignore parsing errors
    }
  }

  return null;
}

/**
 * Detect framework based on dependencies and plugins in the Maven config
 */
function detectFramework(nxConfig: any): string {
  const dependencies = nxConfig.implicitDependencies?.external || [];
  const targets = nxConfig.targets || {};

  // Check for Spring Boot
  if (dependencies.some((dep: string) => dep.includes('spring-boot')) ||
      Object.values(targets).some((target: any) =>
        target.options?.command?.includes('spring-boot'))) {
    return 'spring-boot';
  }

  // Check for Quarkus
  if (dependencies.some((dep: string) => dep.includes('quarkus')) ||
      Object.values(targets).some((target: any) =>
        target.options?.command?.includes('quarkus'))) {
    return 'quarkus';
  }

  // Check for Micronaut
  if (dependencies.some((dep: string) => dep.includes('micronaut'))) {
    return 'micronaut';
  }

  // Check for Jakarta EE
  if (dependencies.some((dep: string) => dep.includes('jakarta'))) {
    return 'jakarta-ee';
  }

  // Check for Maven specific patterns
  if (Object.keys(targets).includes('serve')) {
    return 'web-app';
  }

  return 'maven';
}

/**
 * Normalize target configurations from Maven analysis and add Maven lifecycle phases
 */
function normalizeTargets(targets: Record<string, any>, nxConfig: any, options: MavenPluginOptions): Record<string, TargetConfiguration> {
  const normalizedTargets: Record<string, TargetConfiguration> = {};

  // First add any existing targets from Maven analysis
  for (const [name, target] of Object.entries(targets)) {
    // Map common Maven targets to Nx conventions
    let targetName = name;
    if (name === 'package' && options.buildTargetName !== 'package') {
      targetName = options.buildTargetName!;
    } else if (name === 'test' && options.testTargetName !== 'test') {
      targetName = options.testTargetName!;
    } else if (name === 'serve' && options.serveTargetName !== 'serve') {
      targetName = options.serveTargetName!;
    }

    normalizedTargets[targetName] = {
      executor: target.executor || '@nx/run-commands:run-commands',
      options: target.options || {},
      inputs: target.inputs,
      outputs: target.outputs,
      dependsOn: target.dependsOn,
      configurations: target.configurations,
    };
  }

  // Add Maven lifecycle phase targets and plugin goals from detected data
  const detectedTargets = generateDetectedTargets(nxConfig, options);
  Object.assign(normalizedTargets, detectedTargets);

  return normalizedTargets;
}

/**
 * Generate targets based on pre-calculated data from Java analyzer
 * Uses goalsByPhase and goalDependencies from Java for optimal structure
 */
function generateDetectedTargets(nxConfig: any, options: MavenPluginOptions): Record<string, TargetConfiguration> {
  const detectedTargets: Record<string, TargetConfiguration> = {};
  
  // Get pre-calculated data from Java analyzer
  const relevantPhases = nxConfig.relevantPhases || [];
  const pluginGoals = nxConfig.pluginGoals || [];
  const goalsByPhase = nxConfig.goalsByPhase || {};
  const goalDependencies = nxConfig.goalDependencies || {};
  const crossProjectDependencies = nxConfig.crossProjectDependencies || {};
  
  // Step 1: Generate goal targets using pre-calculated dependencies
  for (const goalInfo of pluginGoals) {
    if (goalInfo.targetName && !detectedTargets[goalInfo.targetName]) {
      const goalTarget = createGoalTarget(goalInfo, options, goalDependencies, crossProjectDependencies);
      if (goalTarget) {
        detectedTargets[goalInfo.targetName] = goalTarget;
      }
    }
  }
  
  // Step 2: Generate phase targets that depend on their own goals (from Java)
  for (const phase of relevantPhases) {
    const phaseTarget = createPhaseTarget(phase, options, goalsByPhase);
    if (phaseTarget) {
      detectedTargets[phase] = phaseTarget;
    }
  }
  
  return detectedTargets;
}


/**
 * Create a target configuration for a Maven phase (aggregator that depends on its own goals)
 */
function createPhaseTarget(phase: string, options: MavenPluginOptions, goalsByPhase: Record<string, string[]>): TargetConfiguration | null {
  const baseInputs = ['{projectRoot}/pom.xml'];
  const baseCommand = `${options.mavenExecutable} ${phase}`;
  
  // Define phase-specific configurations
  const phaseConfig: Record<string, Partial<TargetConfiguration>> = {
    'clean': {
      inputs: baseInputs,
      outputs: [],
    },
    'validate': {
      inputs: baseInputs,
      outputs: [],
    },
    'compile': {
      inputs: ['{projectRoot}/src/main/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/classes/**/*'],
    },
    'test-compile': {
      inputs: ['{projectRoot}/src/test/**/*', '{projectRoot}/src/main/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/test-classes/**/*'],
    },
    'test': {
      inputs: ['{projectRoot}/src/test/**/*', '{projectRoot}/src/main/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/surefire-reports/**/*', '{projectRoot}/target/test-classes/**/*'],
    },
    'package': {
      inputs: ['{projectRoot}/src/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/*.jar', '{projectRoot}/target/*.war'],
    },
    'verify': {
      inputs: ['{projectRoot}/src/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/**/*', '{projectRoot}/target/failsafe-reports/**/*'],
    },
    'install': {
      inputs: ['{projectRoot}/src/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/**/*'],
    },
    'deploy': {
      inputs: ['{projectRoot}/src/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/**/*'],
    },
    'site': {
      inputs: ['{projectRoot}/src/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/site/**/*'],
    },
    'integration-test': {
      inputs: ['{projectRoot}/src/**/*', ...baseInputs],
      outputs: ['{projectRoot}/target/failsafe-reports/**/*'],
    },
  };
  
  const config = phaseConfig[phase] || {
    inputs: baseInputs,
    outputs: [],
  };
  
  const targetConfig: TargetConfiguration = {
    executor: '@nx/run-commands:run-commands',
    options: {
      command: baseCommand,
      cwd: '{projectRoot}',
    },
    metadata: {
      type: 'phase',
      phase: phase,
      technologies: ['maven'],
      description: `Maven lifecycle phase: ${phase} (aggregator)`,
    },
    ...config,
  };
  
  // Phase depends on all its own goals
  const goalsInPhase = goalsByPhase[phase] || [];
  if (goalsInPhase.length > 0) {
    targetConfig.dependsOn = goalsInPhase;
  }
  
  return targetConfig;
}

/**
 * Create a target configuration for a plugin goal using pre-calculated dependencies
 */
function createGoalTarget(goalInfo: any, options: MavenPluginOptions, goalDependencies: Record<string, string[]>, crossProjectDependencies: Record<string, string[]>): TargetConfiguration | null {
  const { pluginKey, goal, targetType, phase } = goalInfo;
  
  // Create the Maven command
  const [groupId, artifactId] = pluginKey.split(':');
  const command = `${options.mavenExecutable} ${groupId}:${artifactId}:${goal}`;
  
  // Create a more user-friendly description
  const pluginName = artifactId.replace('-maven-plugin', '').replace('-plugin', '');
  let description = `${pluginName}:${goal}`;
  
  // Add framework-specific descriptions
  if (pluginKey.includes('quarkus')) {
    switch (goal) {
      case 'dev':
        description = 'Start Quarkus development mode';
        break;
      case 'build':
        description = 'Build Quarkus application';
        break;
      case 'generate-code':
        description = 'Generate Quarkus code';
        break;
      case 'test':
        description = 'Run Quarkus tests';
        break;
      default:
        description = `Quarkus ${goal}`;
    }
  } else if (pluginKey.includes('spring-boot')) {
    switch (goal) {
      case 'run':
        description = 'Start Spring Boot application';
        break;
      case 'build-image':
        description = 'Build Spring Boot Docker image';
        break;
      case 'repackage':
        description = 'Repackage Spring Boot application';
        break;
      default:
        description = `Spring Boot ${goal}`;
    }
  } else if (pluginKey.includes('surefire')) {
    description = 'Run unit tests';
  } else if (pluginKey.includes('failsafe')) {
    description = 'Run integration tests';
  }

  // Base configuration
  const baseConfig: TargetConfiguration = {
    executor: '@nx/run-commands:run-commands',
    options: {
      command,
      cwd: '{projectRoot}',
    },
    metadata: {
      type: 'goal',
      plugin: pluginKey,
      goal: goal,
      targetType: targetType,
      phase: phase !== 'null' ? phase : undefined,
      technologies: ['maven'],
      description: description,
    },
    inputs: ['{projectRoot}/pom.xml'],
    outputs: [],
  };
  
  // Customize based on target type
  switch (targetType) {
    case 'serve':
      baseConfig.inputs!.push('{projectRoot}/src/**/*');
      break;
    case 'build':
      baseConfig.inputs!.push('{projectRoot}/src/**/*');
      baseConfig.outputs = ['{projectRoot}/target/**/*'];
      break;
    case 'test':
      baseConfig.inputs!.push('{projectRoot}/src/test/**/*', '{projectRoot}/src/main/**/*');
      baseConfig.outputs = ['{projectRoot}/target/surefire-reports/**/*'];
      break;
    case 'deploy':
      baseConfig.inputs!.push('{projectRoot}/src/**/*');
      break;
    case 'utility':
      baseConfig.inputs!.push('{projectRoot}/src/**/*');
      break;
  }
  
  // Get goal-to-goal dependencies from Java analyzer
  const targetName = goalInfo.targetName || goal;
  const goalDeps = goalDependencies[targetName] || [];
  
  // Get cross-project dependencies for this target
  const crossProjectDeps = crossProjectDependencies[targetName] || [];
  
  // Merge goal dependencies and cross-project dependencies
  const allDependencies = [...goalDeps, ...crossProjectDeps];
  if (allDependencies.length > 0) {
    // Resolve cross-project dependencies with fallbacks
    const resolvedDeps = allDependencies.map(dep => {
      if (dep.includes(':')) {
        return resolveCrossProjectDependency(dep);
      }
      return dep;
    }).filter(dep => dep !== null);
    
    if (resolvedDeps.length > 0) {
      baseConfig.dependsOn = resolvedDeps;
    }
  }
  
  return baseConfig;
}

/**
 * Resolve cross-project dependency with fallback support
 * Handles dependencies like "projectA:package|compile|validate"
 */
function resolveCrossProjectDependency(dependency: string): string {
  if (!dependency.includes(':')) {
    return dependency; // Not a cross-project dependency
  }
  
  const [project, fallbackChain] = dependency.split(':', 2);
  if (!fallbackChain || !fallbackChain.includes('|')) {
    return dependency; // No fallback, return as-is
  }
  
  // For now, just return the first target in the fallback chain
  // In a complete implementation, this would check which targets actually exist
  const targets = fallbackChain.split('|');
  return `${project}:${targets[0]}`;
}

/**
 * Get dependencies for a Maven phase
 */
function getPhaseDependencies(phase: string): string[] {
  const phaseDependencies: Record<string, string[]> = {
    'test': ['compile'],
    'package': ['test'],
    'verify': ['package'],
    'install': ['verify'],
    'deploy': ['install'],
    'integration-test': ['package'],
  };
  
  return phaseDependencies[phase] || [];
}

/**
 * Plugin configuration for registration
 */
export default {
  name: 'maven-plugin2',
  createNodesV2,
  createDependencies,
};
