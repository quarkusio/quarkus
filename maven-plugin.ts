import {
    CreateDependencies,
    CreateNodesContext,
    createNodesFromFiles,
    CreateNodesFunction,
    ProjectConfiguration,
    readJsonFile,
    TargetConfiguration,
    writeJsonFile,
    DependencyType,
} from '@nx/devkit';
import {calculateHashForCreateNodes} from '@nx/devkit/src/utils/calculate-hash-for-create-nodes';
import {existsSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {workspaceDataDirectory} from 'nx/src/utils/cache-directory';
import {hashObject} from 'nx/src/devkit-internals';
import {MavenProjectInfo, MavenReport, parsePomXml} from './parse-pom-xml';
import {ChildProcess} from 'node:child_process';
import type {RawProjectGraphDependency} from "nx/src/project-graph/project-graph-builder";

// Performance caches
const dependencyCache = new Map<string, string[]>();
const projectTypeCache = new Map<string, 'application' | 'library'>();
const frameworkCache = new Map<string, { isQuarkus: boolean; isSpringBoot: boolean }>();

const MAVEN_CONFIG_GLOB = '**/pom.xml';
const MAVEN_CONFIG_AND_TEST_GLOB = '{**/pom.xml,**/src/test/**/*.java}';

interface MavenPluginOptions {
    includeSubmodules?: boolean;
    verifyTargetName?: string;
    runTargetName?: string;
    testTargetName?: string;
    compileTargetName?: string;
    packageTargetName?: string;
    installTargetName?: string;
}

function normalizeOptions(options: MavenPluginOptions): MavenPluginOptions {
    options ??= {};
    options.verifyTargetName ??= 'verify';
    options.runTargetName ??= 'run';
    options.testTargetName ??= 'test';
    options.compileTargetName ??= 'compile';
    options.packageTargetName ??= 'package';
    options.installTargetName ??= 'install';
    return options;
}

type MavenTargets = Record<string, Partial<ProjectConfiguration>>;

function readTargetsCache(cachePath: string): MavenTargets {
    return existsSync(cachePath) ? readJsonFile(cachePath) : {};
}

function writeTargetsToCache(cachePath: string, results: MavenTargets) {
    writeJsonFile(cachePath, results);
}

export const createNodesV2 = [
    MAVEN_CONFIG_AND_TEST_GLOB,
    async (files, options, context) => {
        console.log(`[Maven Plugin] createNodesV2 called with ${files.length} files`);

        const {pomFiles, testFiles} = splitConfigFiles(files);
        console.log(`[Maven Plugin] Processing ${pomFiles.length} pom.xml files and ${testFiles.length} test files`);

        const optionsHash = hashObject(options);
        const cachePath = join(workspaceDataDirectory, `maven-${optionsHash}.hash`);
        const targetsCache = readTargetsCache(cachePath);

        console.log(`[Maven Plugin] Parsing POM files for project creation...`);
        const startTime = Date.now();
        const mavenReport = await parsePomXml(context.workspaceRoot, pomFiles);
        const parseTime = Date.now() - startTime;
        console.log(`[Maven Plugin] Parsed ${mavenReport.projects.size} POMs in ${parseTime}ms`);

        // Instead of using all globbed files, discover modules recursively from root
        console.log(`[Maven Plugin] Discovering Maven modules recursively from root pom.xml...`);
        const actualPomFiles = await discoverMavenModules(context.workspaceRoot);
        console.log(`[Maven Plugin] Found ${actualPomFiles.length} actual Maven projects (vs ${pomFiles.length} globbed)`);

        // Parse only the actual projects
        const actualMavenReport = await parsePomXml(context.workspaceRoot, actualPomFiles);
        console.log(`[Maven Plugin] Parsed ${actualMavenReport.projects.size} actual Maven projects`);

        const mavenProjectRootToTestFilesMap = getMavenProjectRootToTestFilesMap(
            testFiles,
            actualPomFiles.map((f) => dirname(f))
        );

        // Collect and dedupe all unknown plugins before discovery
        console.log(`[Maven Plugin] Collecting unique plugins for batch discovery...`);
        const uniquePlugins = collectUniquePlugins(actualMavenReport, actualPomFiles);
        console.log(`[Maven Plugin] Found ${uniquePlugins.size} unique unknown plugins to discover`);

        // Skip plugin discovery for now to improve performance
        console.log(`[Maven Plugin] Skipping plugin discovery for performance - using known plugins only`);
        const discoveredPlugins = new Map<string, DiscoveredPlugin>();

        try {
            return createNodesFromFiles(
                makeCreateNodesForMavenConfigFile(
                    actualMavenReport,
                    targetsCache,
                    mavenProjectRootToTestFilesMap,
                    discoveredPlugins
                ),
                actualPomFiles,
                options,
                context
            );
        } finally {
            writeTargetsToCache(cachePath, targetsCache);
        }
    },
];

export const createDependencies: CreateDependencies =
    async (options, context) => {
        // Get all pom.xml files from the workspace - for full dependency analysis
        const allProjects = Object.values(context.projects);
        const allPomFiles: string[] = [];

        for (const project of allProjects) {
            const pomPath = `${project.root}/pom.xml`;
            // Only include if the pom.xml file actually exists
            if (existsSync(pomPath)) {
                allPomFiles.push(pomPath);
            }
        }

        console.log(`[Maven Plugin] createDependencies called, processing ${allPomFiles.length} Maven projects`);
        console.log(`[Maven Plugin] Available projects in context: ${Object.keys(context.projects).length}`);
        const projectNames = Object.values(context.projects).map(p => p.name).slice(0, 5);
        console.log(`[Maven Plugin] Sample project names: ${projectNames.join(', ')}`);

        if (allPomFiles.length === 0) {
            console.log('[Maven Plugin] No Maven projects found, returning empty dependencies');
            return [];
        }

        const {pomFiles} = splitConfigFiles(allPomFiles);
        console.log(`[Maven Plugin] Found ${pomFiles.length} pom.xml files to process`);

        if (pomFiles.length === 0) {
            console.log('[Maven Plugin] No pom.xml files found, returning empty dependencies');
            return [];
        }

        const startTime = Date.now();
        console.log(`[Maven Plugin] Parsing ${pomFiles.length} POM files with selective effective POM usage...`);

        // Use effective POM only for core projects that likely have complex inheritance
        const useEffectiveForProject = (pomFile: string): boolean => {
          return pomFile.includes('/core/') ||
                 pomFile.includes('/extensions/') ||
                 pomFile.includes('/independent-projects/arc/');
        };

        // Split POMs into those needing effective parsing vs raw parsing
        const effectivePomFiles = pomFiles.filter(useEffectiveForProject);
        const rawPomFiles = pomFiles.filter(f => !useEffectiveForProject(f));

        console.log(`[Maven Plugin] Using effective POM for ${effectivePomFiles.length} core projects, raw POM for ${rawPomFiles.length} others`);

        // Parse both sets and merge
        const [effectiveReport, rawReport] = await Promise.all([
          effectivePomFiles.length > 0 ? parsePomXml(context.workspaceRoot, effectivePomFiles, true) : { projects: new Map(), projectToModules: new Map() },
          rawPomFiles.length > 0 ? parsePomXml(context.workspaceRoot, rawPomFiles, false) : { projects: new Map(), projectToModules: new Map() }
        ]);

        // Merge the reports
        const mavenReport = {
          projects: new Map([...effectiveReport.projects, ...rawReport.projects]),
          projectToModules: new Map([...effectiveReport.projectToModules, ...rawReport.projectToModules])
        };

        const parseTime = Date.now() - startTime;
        console.log(`[Maven Plugin] Parsed ${mavenReport.projects.size} projects in ${parseTime}ms`);

        const dependencies: RawProjectGraphDependency[] = [];
        let processedProjects = 0;
        const depStartTime = Date.now();

        // Pre-build coordinate lookup map for faster dependency resolution
        const coordinateMap = new Map<string, string>();
        for (const [pomFile, projectInfo] of mavenReport.projects) {
            const coordinate = `${projectInfo.groupId ?? projectInfo.parent?.groupId}.${projectInfo.artifactId}`;
            coordinateMap.set(coordinate, coordinate);
        }

        for (const pomFile of pomFiles) {
            const projectInfo = mavenReport.projects.get(pomFile);
            if (!projectInfo) continue;

            processedProjects++;
            if (processedProjects % 200 === 0) {
                console.log(`[Maven Plugin] Processed ${processedProjects}/${pomFiles.length} projects...`);
            }

            const projectRoot = dirname(pomFile);
            const sourceProjectName = `${projectInfo.groupId ?? projectInfo.parent?.groupId}.${projectInfo.artifactId}`;

            // Use cached dependency resolution
            const cacheKey = `${sourceProjectName}-deps`;
            let allDependencies = dependencyCache.get(cacheKey);

            if (!allDependencies) {
                // Create runtime dependencies (compile + runtime scope)
                const runtimeDependencies = createProjectDependenciesOptimized(projectInfo, coordinateMap, false);

                // Create test dependencies (test scope)
                const testDependencies = createProjectDependenciesOptimized(projectInfo, coordinateMap, true);

                allDependencies = [...new Set([...runtimeDependencies, ...testDependencies])];
                dependencyCache.set(cacheKey, allDependencies);
            }

            // Add dependencies
            for (const targetProject of allDependencies) {
                // Verify target project exists in context
                const targetExists = Object.values(context.projects).some(p =>
                    p.name === targetProject ||
                    ((p.metadata as any)?.maven && `${(p.metadata as any).maven.groupId}.${(p.metadata as any).maven.artifactId}` === targetProject)
                );

                if (targetExists) {
                    // Make sourceFile relative to workspace root
                    const relativeSourceFile = pomFile.startsWith(context.workspaceRoot) 
                        ? pomFile.substring(context.workspaceRoot.length + 1)
                        : pomFile;
                    dependencies.push({
                        source: sourceProjectName,
                        target: targetProject,
                        type: DependencyType.static,
                        sourceFile: relativeSourceFile,
                    });
                    console.log(`[Maven Plugin] Added dependency: ${sourceProjectName} -> ${targetProject}`);
                } else {
                    console.log(`[Maven Plugin] Skipping dependency to non-existent project: ${targetProject} (source: ${sourceProjectName})`);
                }
            }

            // Create dependencies from parent relationships
            if (projectInfo.parent) {
                const parentCoordinate = `${projectInfo.parent.groupId}.`;
                const parentProjectName = coordinateMap.get(parentCoordinate);
                if (parentProjectName && Object.values(context.projects).some(p => p.name === parentProjectName)) {
                    // Make sourceFile relative to workspace root
                    const relativeSourceFile = pomFile.startsWith(context.workspaceRoot) 
                        ? pomFile.substring(context.workspaceRoot.length + 1)
                        : pomFile;
                    dependencies.push({
                        source: sourceProjectName,
                        target: parentProjectName,
                        type: DependencyType.implicit,
                        sourceFile: relativeSourceFile,
                    });
                }
            }

            // Create dependencies from module relationships (parent -> modules)
            if (projectInfo.modules) {
                for (const module of projectInfo.modules) {
                    const modulePath = join(projectRoot, module, 'pom.xml');
                    const moduleProject = mavenReport.projects.get(modulePath);
                    if (moduleProject) {
                        const moduleProjectName = `${moduleProject.groupId ?? moduleProject.parent?.groupId}.${moduleProject.artifactId}`;
                        if (Object.values(context.projects).some(p => p.name === moduleProjectName)) {
                            // Make sourceFile relative to workspace root
                            const relativeSourceFile = pomFile.startsWith(context.workspaceRoot) 
                                ? pomFile.substring(context.workspaceRoot.length + 1)
                                : pomFile;
                            dependencies.push({
                                source: sourceProjectName,
                                target: moduleProjectName,
                                type: DependencyType.implicit,
                                sourceFile: relativeSourceFile,
                            });
                        }
                    }
                }
            }
        }

        const depTime = Date.now() - depStartTime;
        console.log(`[Maven Plugin] Created ${dependencies.length} dependencies from ${processedProjects} projects in ${depTime}ms`);
        console.log(`[Maven Plugin] Sample dependencies: ${dependencies.slice(0, 3).map(d => `${d.source} -> ${d.target}`).join(', ')}`);
        console.log(`[Maven Plugin] RETURNING DEPENDENCIES ARRAY TO NX, length: ${dependencies.length}`);

        // Validate that all source and target projects exist
        const missingSourceProjects = new Set<string>();
        const missingTargetProjects = new Set<string>();

        for (const dep of dependencies.slice(0, 10)) { // Check first 10 for sample
            const sourceExists = Object.values(context.projects).some(p => p.name === dep.source);
            const targetExists = Object.values(context.projects).some(p => p.name === dep.target);

            if (!sourceExists) {
                missingSourceProjects.add(dep.source);
            }
            if (!targetExists) {
                missingTargetProjects.add(dep.target);
            }
        }

        if (missingSourceProjects.size > 0) {
            console.log(`[Maven Plugin] WARNING: Missing source projects: ${Array.from(missingSourceProjects).join(', ')}`);
        }
        if (missingTargetProjects.size > 0) {
            console.log(`[Maven Plugin] WARNING: Missing target projects: ${Array.from(missingTargetProjects).join(', ')}`);
        }

        return dependencies;
    };

export const makeCreateNodesForMavenConfigFile =
    (
        mavenReport: MavenReport,
        targetsCache: MavenTargets = {},
        mavenProjectRootToTestFilesMap: Record<string, string[]> = {},
        discoveredPlugins: Map<string, DiscoveredPlugin> = new Map()
    ): CreateNodesFunction =>
        async (
            pomFilePath,
            options: MavenPluginOptions | undefined,
            context: CreateNodesContext
        ) => {
            const projectRoot = dirname(pomFilePath);
            options = normalizeOptions(options);

            const hash = await calculateHashForCreateNodes(
                projectRoot,
                options ?? {},
                context
            );
            targetsCache[hash] ??= await createMavenProject(
                mavenReport,
                pomFilePath,
                options,
                context,
                mavenProjectRootToTestFilesMap[projectRoot],
                discoveredPlugins
            );
            const project = targetsCache[hash];
            if (!project) {
                return {};
            }
            return {
                projects: {
                    [projectRoot]: project,
                },
            };
        };

async function createMavenProject(
    mavenReport: MavenReport,
    pomFilePath: string,
    options: MavenPluginOptions | undefined,
    context: CreateNodesContext,
    testFiles: string[] = [],
    discoveredPlugins: Map<string, DiscoveredPlugin> = new Map()
) {
    try {
        const projectInfo = mavenReport.projects.get(pomFilePath);
        if (!projectInfo) {
            return;
        }

        const {targets, targetGroups} = await createMavenTargets(
            projectInfo,
            options,
            context,
            pomFilePath,
            testFiles,
            discoveredPlugins
        );

        const implicitDependencies = createImplicitDependencies(projectInfo, mavenReport, pomFilePath);

        const projectName = `${projectInfo.groupId ?? projectInfo.parent?.groupId}.${
            projectInfo.artifactId
        }`;
        console.log(`[Maven Plugin] Creating project: ${projectName} at ${dirname(pomFilePath)}`);

        const project: Partial<ProjectConfiguration> = {
            name: projectName,
            projectType: determineProjectType(projectInfo),
            targets,
            implicitDependencies,
            metadata: {
                targetGroups,
                technologies: ['maven'],
                maven: {
                    groupId: projectInfo.groupId ?? projectInfo.parent?.groupId,
                    artifactId: projectInfo.artifactId,
                    version: projectInfo.version ?? projectInfo.parent?.version,
                    packaging: projectInfo.packaging,
                },
            } as any,
        };

        return project;
    } catch (e) {
        console.error(e);
        return undefined;
    }
}

async function createMavenTargets(
    projectInfo: MavenProjectInfo,
    options: MavenPluginOptions | undefined,
    context: CreateNodesContext,
    pomFilePath: string,
    testFiles: string[] = [],
    discoveredPlugins: Map<string, DiscoveredPlugin> = new Map()
): Promise<{
    targetGroups: Record<string, string[]>;
    targets: Record<string, TargetConfiguration>;
}> {
    const targets: Record<string, TargetConfiguration> = {};
    const targetGroups: Record<string, string[]> = {
        build: [],
        test: [],
        dev: [],
        quality: [],
    };

    const projectRoot = dirname(pomFilePath);
    const hasTests = testFiles.length > 0;

    // Create enhanced inputs/outputs based on project structure
    const commonInputs = getProjectInputs(projectInfo, projectRoot);
    const commonOutputs = getProjectOutputs(projectInfo, projectRoot);

    // Add standard Maven lifecycle targets with enhanced configuration
    const lifecycleTargets = [
        {
            name: options.compileTargetName,
            phase: 'compile',
            group: 'build',
            outputs: ['{projectRoot}/target/classes'],
        },
        {
            name: options.testTargetName,
            phase: 'test',
            group: 'test',
            outputs: hasTests ? ['{projectRoot}/target/test-results', '{projectRoot}/target/surefire-reports'] : undefined,
            condition: hasTests,
        },
        {
            name: options.packageTargetName,
            phase: 'package',
            group: 'build',
            outputs: [`{projectRoot}/target/*.${projectInfo.packaging || 'jar'}`],
        },
        {
            name: options.verifyTargetName,
            phase: 'verify',
            group: 'quality',
            outputs: undefined,
        },
        {
            name: options.installTargetName,
            phase: 'install',
            group: 'build',
            outputs: undefined,
        },
    ];

    // Add lifecycle targets
    for (const target of lifecycleTargets) {
        if (target.condition === false) continue;

        const targetConfig: TargetConfiguration = {
            command: `mvn ${target.phase}`,
            options: {
                cwd: projectRoot,
            },
            cache: true,
            inputs: commonInputs,
            outputs: target.outputs,
            metadata: {
                technologies: ['maven'],
                description: `Run Maven ${target.phase} phase`,
            },
        };

        targets[target.name] = targetConfig;
        targetGroups[target.group].push(target.name);
    }

    // Dynamically discover and add plugin-specific targets
    await addDiscoveredPluginTargets(projectInfo, targets, targetGroups, projectRoot, options, discoveredPlugins);

    return {targets, targetGroups};
}

// Plugin discovery interfaces
interface PluginGoal {
    goal: string;
    description: string;
    phase?: string;
}

interface DiscoveredPlugin {
    groupId: string;
    artifactId: string;
    version: string;
    goals: PluginGoal[];
}

// Cache for plugin discovery results
const pluginGoalsCache = new Map<string, DiscoveredPlugin>();

// Process pool for controlled concurrent plugin discovery
class PluginDiscoveryPool {
    private activeRequests = 0;
    private readonly maxConcurrent = 6; // Increased to 6 concurrent processes for better performance
    private readonly queue: Array<{ pluginKey: string; projectRoot: string; resolve: (result: DiscoveredPlugin | null) => void }> = [];
    private mavenCommand: string | null = null;
    private activeProcesses = new Set<ChildProcess>(); // Track active child processes for cleanup

    async discover(pluginKey: string, projectRoot: string): Promise<DiscoveredPlugin | null> {
        // Check cache first
        if (pluginGoalsCache.has(pluginKey)) {
            console.log(`[Maven Plugin Discovery] Using cached result for ${pluginKey}`);
            return pluginGoalsCache.get(pluginKey)!;
        }

        // Initialize maven command on first use
        if (this.mavenCommand === null) {
            await this.initializeMavenCommand();
        }

        console.log(`[Maven Plugin Discovery] Queueing discovery request for ${pluginKey}`);
        return new Promise((resolve) => {
            this.queue.push({ pluginKey, projectRoot, resolve });
            this.processQueue();
        });
    }

    private async initializeMavenCommand(): Promise<void> {
        try {
            const { exec } = await import('child_process');
            const { promisify } = await import('util');
            const execAsync = promisify(exec);

            await execAsync('which mvnd', { timeout: 1000 });
            this.mavenCommand = 'mvnd help:describe';
            console.log(`[Maven Plugin Discovery] Maven Daemon (mvnd) detected - will use for faster execution`);
        } catch {
            this.mavenCommand = 'mvn help:describe';
            console.log(`[Maven Plugin Discovery] Using standard Maven (mvn) - consider installing Maven Daemon for faster execution`);
        }
    }

    private async processQueue() {
        if (this.activeRequests >= this.maxConcurrent || this.queue.length === 0) {
            return;
        }

        const request = this.queue.shift()!;
        this.activeRequests++;

        console.log(`[Maven Plugin Discovery] Starting discovery for ${request.pluginKey} (${this.activeRequests}/${this.maxConcurrent} active, ${this.queue.length} queued)`);

        try {
            const result = await this.executeDiscovery(request.pluginKey, request.projectRoot);
            if (result) {
                console.log(`[Maven Plugin Discovery] Successfully discovered ${result.goals.length} goals for ${request.pluginKey}`);
            } else {
                console.log(`[Maven Plugin Discovery] No goals found for ${request.pluginKey}`);
            }
            request.resolve(result);
        } catch (error) {
            console.warn(`[Maven Plugin Discovery] Failed to discover goals for ${request.pluginKey}:`, error.message);
            request.resolve(null);
        } finally {
            this.activeRequests--;
            console.log(`[Maven Plugin Discovery] Completed discovery for ${request.pluginKey} (${this.activeRequests}/${this.maxConcurrent} active, ${this.queue.length} queued)`);
            // Process next item in queue immediately for better performance
            setImmediate(() => this.processQueue());
        }
    }

    private async executeDiscovery(pluginKey: string, projectRoot: string): Promise<DiscoveredPlugin | null> {
        let childProcess: ChildProcess | null = null;
        try {
            console.log(`[Maven Plugin Discovery] Executing plugin discovery for ${pluginKey} in ${projectRoot}`);
            const startTime = Date.now();

            const { spawn } = await import('child_process');

            // Use spawn instead of exec for better process control
            const args = [
                'help:describe',
                `-Dplugin=${pluginKey}`,
                '-Ddetail=false',
                '-q',
                '-o',
                '-B',
                '--no-transfer-progress',
                `-Dmaven.repo.local=${process.env.HOME}/.m2/repository`
            ];

            const command = this.mavenCommand!.split(' ')[0]; // 'mvn' or 'mvnd'

            childProcess = spawn(command, args, {
                cwd: projectRoot,
                stdio: ['ignore', 'pipe', 'pipe'],
                env: {
                    ...process.env,
                    'MAVEN_OPTS': '-Xmx128m -XX:+UseParallelGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.awt.headless=true -Dfile.encoding=UTF-8'
                }
            });

            // Track the process for cleanup
            this.activeProcesses.add(childProcess);

            return new Promise((resolve) => {
                let stdout = '';
                let stderr = '';
                let isResolved = false;

                const resolveOnce = (result: DiscoveredPlugin | null) => {
                    if (!isResolved) {
                        isResolved = true;
                        if (childProcess) {
                            this.activeProcesses.delete(childProcess);
                        }
                        resolve(result);
                    }
                };

                if (childProcess?.stdout) {
                    childProcess.stdout.on('data', (data: Buffer) => {
                        stdout += data.toString();
                    });
                }

                if (childProcess?.stderr) {
                    childProcess.stderr.on('data', (data: Buffer) => {
                        stderr += data.toString();
                    });
                }

                childProcess?.on('close', (code: number | null) => {
                    const execTime = Date.now() - startTime;

                    if (code === 0) {
                        console.log(`[Maven Plugin Discovery] Maven command completed for ${pluginKey} in ${execTime}ms`);

                        if (stdout.trim()) {
                            console.log(`[Maven Plugin Discovery] Raw stdout for ${pluginKey}:`, JSON.stringify(stdout.substring(0, 500)));

                            const goals = parsePluginDescribeOutput(stdout);
                            console.log(`[Maven Plugin Discovery] Parsed ${goals.length} goals from output for ${pluginKey}:`, goals.map(g => g.goal).join(', '));

                            if (goals.length > 0) {
                                const [groupId, artifactId] = pluginKey.split(':');

                                const discoveredPlugin: DiscoveredPlugin = {
                                    groupId,
                                    artifactId,
                                    version: 'unknown',
                                    goals
                                };

                                pluginGoalsCache.set(pluginKey, discoveredPlugin);
                                console.log(`[Maven Plugin Discovery] Cached discovery result for ${pluginKey}`);
                                resolveOnce(discoveredPlugin);
                                return;
                            }
                        }
                    } else {
                        console.error(`[Maven Plugin Discovery] Process exited with code ${code} for ${pluginKey}. stderr:`, stderr);
                    }

                    resolveOnce(null);
                });

                childProcess?.on('error', (error: Error) => {
                    console.error(`[Maven Plugin Discovery] Process error for ${pluginKey}:`, error.message);
                    resolveOnce(null);
                });

                // Set timeout
                const timeoutId = setTimeout(() => {
                    if (!isResolved && childProcess && this.activeProcesses.has(childProcess)) {
                        console.warn(`[Maven Plugin Discovery] Timeout for ${pluginKey}, killing process`);
                        try {
                            childProcess.kill('SIGTERM');
                            setTimeout(() => {
                                if (childProcess && childProcess.pid && !childProcess.killed) {
                                    childProcess.kill('SIGKILL');
                                }
                            }, 2000);
                        } catch (error: any) {
                            console.warn(`[Maven Plugin Discovery] Error killing timed out process:`, error?.message);
                        }
                        resolveOnce(null);
                    }
                }, 15000); // Increased timeout to 15 seconds

                // Clear timeout if process completes
                childProcess?.on('close', () => clearTimeout(timeoutId));
            });

        } catch (error: any) {
            if (childProcess && this.activeProcesses.has(childProcess)) {
                try {
                    childProcess.kill('SIGTERM');
                } catch (killError: any) {
                    console.warn(`[Maven Plugin Discovery] Error killing process:`, killError?.message);
                }
                this.activeProcesses.delete(childProcess);
            }
            console.error(`[Maven Plugin Discovery] Error during discovery for ${pluginKey}:`, error?.message || error, error?.code || '');
            return null;
        }
    }

    cleanup(): void {
        console.log(`[Maven Plugin Discovery] Cleaning up ${this.activeProcesses.size} active processes`);
        for (const process of this.activeProcesses) {
            try {
                if (process && process.pid && !process.killed) {
                    console.log(`[Maven Plugin Discovery] Killing process ${process.pid}`);
                    process.kill('SIGTERM');
                    setTimeout(() => {
                        if (process.pid && !process.killed) {
                            console.log(`[Maven Plugin Discovery] Force killing process ${process.pid}`);
                            process.kill('SIGKILL');
                        }
                    }, 2000);
                }
            } catch (error: any) {
                console.warn(`[Maven Plugin Discovery] Error cleaning up process:`, error?.message || error);
            }
        }
        this.activeProcesses.clear();
    }
}

const pluginDiscoveryPool = new PluginDiscoveryPool();

// Ensure cleanup on process exit
process.on('exit', () => {
    pluginDiscoveryPool.cleanup();
});

process.on('SIGINT', () => {
    console.log('\n[Maven Plugin Discovery] Received SIGINT, cleaning up...');
    pluginDiscoveryPool.cleanup();
    process.exit(0);
});

process.on('SIGTERM', () => {
    console.log('\n[Maven Plugin Discovery] Received SIGTERM, cleaning up...');
    pluginDiscoveryPool.cleanup();
    process.exit(0);
});

process.on('uncaughtException', (error) => {
    console.error('[Maven Plugin Discovery] Uncaught exception, cleaning up...', error);
    pluginDiscoveryPool.cleanup();
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('[Maven Plugin Discovery] Unhandled rejection, cleaning up...', reason);
    pluginDiscoveryPool.cleanup();
});

// Common plugin configurations for well-known plugins
const PLUGIN_CONFIGS = new Map([
    ['org.springframework.boot:spring-boot-maven-plugin', {
        goals: [
            { goal: 'run', description: 'Run Spring Boot application', category: 'dev' },
            { goal: 'start', description: 'Start Spring Boot application', category: 'dev' },
            { goal: 'stop', description: 'Stop Spring Boot application', category: 'dev' },
            { goal: 'build-image', description: 'Build OCI image', category: 'build' }
        ]
    }],
    ['io.quarkus:quarkus-maven-plugin', {
        goals: [
            { goal: 'dev', description: 'Start Quarkus in development mode', category: 'dev' },
            { goal: 'build', description: 'Build Quarkus application', category: 'build' },
            { goal: 'generate-code', description: 'Generate sources', category: 'build' },
            { goal: 'generate-code-tests', description: 'Generate test sources', category: 'test' }
        ]
    }],
    ['org.flywaydb:flyway-maven-plugin', {
        goals: [
            { goal: 'migrate', description: 'Run database migrations', category: 'build' },
            { goal: 'info', description: 'Show migration info', category: 'quality' },
            { goal: 'validate', description: 'Validate migrations', category: 'quality' },
            { goal: 'clean', description: 'Clean database', category: 'build' }
        ]
    }],
    ['org.liquibase:liquibase-maven-plugin', {
        goals: [
            { goal: 'update', description: 'Update database schema', category: 'build' },
            { goal: 'status', description: 'Show change log status', category: 'quality' },
            { goal: 'validate', description: 'Validate change log', category: 'quality' }
        ]
    }],
    ['com.diffplug.spotless:spotless-maven-plugin', {
        goals: [
            { goal: 'apply', description: 'Format code with Spotless', category: 'quality' },
            { goal: 'check', description: 'Check code formatting', category: 'quality' }
        ]
    }],
    ['org.apache.maven.plugins:maven-checkstyle-plugin', {
        goals: [
            { goal: 'check', description: 'Run Checkstyle code analysis', category: 'quality' },
            { goal: 'checkstyle', description: 'Generate Checkstyle report', category: 'quality' }
        ]
    }],
    ['com.github.spotbugs:spotbugs-maven-plugin', {
        goals: [
            { goal: 'check', description: 'Run SpotBugs static analysis', category: 'quality' },
            { goal: 'spotbugs', description: 'Generate SpotBugs report', category: 'quality' }
        ]
    }]
]);

async function addDiscoveredPluginTargets(
    projectInfo: MavenProjectInfo,
    targets: Record<string, TargetConfiguration>,
    targetGroups: Record<string, string[]>,
    projectRoot: string,
    options: MavenPluginOptions | undefined,
    discoveredPlugins: Map<string, DiscoveredPlugin>
): Promise<void> {
    if (!projectInfo.plugins) return;

    for (const plugin of projectInfo.plugins) {
        const pluginKey = `${plugin.groupId}:${plugin.artifactId}`;
        const pluginConfig = PLUGIN_CONFIGS.get(pluginKey);

        if (pluginConfig) {
            // Use known configuration for common plugins
            for (const goalConfig of pluginConfig.goals) {
                const targetName = getTargetName(plugin.artifactId, goalConfig.goal);
                const category = goalConfig.category || 'build';

                targets[targetName] = {
                    command: `mvn ${pluginKey}:${goalConfig.goal}`,
                    options: { cwd: projectRoot },
                    cache: shouldCacheTarget(goalConfig.goal),
                    inputs: getInputsForPlugin(pluginKey, goalConfig.goal, projectRoot),
                    outputs: getOutputsForPlugin(pluginKey, goalConfig.goal, projectRoot),
                    metadata: {
                        technologies: ['maven', getPluginTechnology(plugin.artifactId)],
                        description: goalConfig.description,
                    },
                };

                targetGroups[category] = targetGroups[category] || [];
                targetGroups[category].push(targetName);
            }
        } else {
            // Use pre-discovered plugin data
            const discoveredPlugin = discoveredPlugins.get(pluginKey);
            if (discoveredPlugin) {
                for (const goal of discoveredPlugin.goals) {
                    const targetName = getTargetName(plugin.artifactId, goal.goal);

                    targets[targetName] = {
                        command: `mvn ${pluginKey}:${goal.goal}`,
                        options: { cwd: projectRoot },
                        cache: true, // Default to cached for unknown plugins
                        inputs: ['{projectRoot}/src/**/*', '{projectRoot}/pom.xml'],
                        metadata: {
                            technologies: ['maven'],
                            description: goal.description || `Run ${goal.goal} goal`,
                        },
                    };

                    targetGroups.build = targetGroups.build || [];
                    targetGroups.build.push(targetName);
                }
            }
        }
    }
}


function parsePluginDescribeOutput(output: string): PluginGoal[] {
    const goals: PluginGoal[] = [];
    const lines = output.split('\n');

    let inGoalsSection = false;
    let foundGoalsHeader = false;

    console.log(`[Maven Plugin Discovery] Parsing ${lines.length} lines of output`);

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmedLine = line.trim();

        // Skip empty lines
        if (!trimmedLine) continue;

        // Look for various goals section indicators
        if (!foundGoalsHeader && (
            trimmedLine.toLowerCase().includes('goals are available') ||
            trimmedLine.toLowerCase().includes('this plugin has') ||
            trimmedLine.toLowerCase().includes('goals:') ||
            trimmedLine.includes('Mojo:') ||
            (trimmedLine.includes(':') && (trimmedLine.includes('goal') || trimmedLine.includes('mojo')))
        )) {
            foundGoalsHeader = true;
            inGoalsSection = true;
            console.log(`[Maven Plugin Discovery] Found goals section header at line ${i}: "${trimmedLine}"`);
            continue;
        }

        if (foundGoalsHeader) {
            // Try different goal parsing patterns

            // Pattern 1: "goalname - description"
            let goalMatch = trimmedLine.match(/^([a-zA-Z0-9\-_.]+)\s*-\s*(.+)$/);
            if (goalMatch) {
                const goal = goalMatch[1].trim();
                const description = goalMatch[2].trim();
                goals.push({ goal, description });
                console.log(`[Maven Plugin Discovery] Found goal (pattern 1): ${goal} - ${description}`);
                continue;
            }

            // Pattern 2: "groupId:artifactId:version:goalname description"
            goalMatch = trimmedLine.match(/^[^:]+:[^:]+:[^:]+:([a-zA-Z0-9\-_.]+)(?:\s+(.*))?$/);
            if (goalMatch) {
                const goal = goalMatch[1].trim();
                const description = goalMatch[2]?.trim() || `Run ${goal} goal`;
                goals.push({ goal, description });
                console.log(`[Maven Plugin Discovery] Found goal (pattern 2): ${goal} - ${description}`);
                continue;
            }

            // Pattern 3: "goalname" (standalone)
            goalMatch = trimmedLine.match(/^([a-zA-Z0-9\-_.]+)$/);
            if (goalMatch && goalMatch[1].length > 1 && !goalMatch[1].includes('.')) {
                const goal = goalMatch[1].trim();
                const description = `Run ${goal} goal`;
                goals.push({ goal, description });
                console.log(`[Maven Plugin Discovery] Found goal (pattern 3): ${goal} - ${description}`);
                continue;
            }

            // Pattern 4: Mojo entries "goal (implementation: ClassName)"
            goalMatch = trimmedLine.match(/^([a-zA-Z0-9\-_.]+)\s*\(implementation:\s*[^)]+\)$/);
            if (goalMatch) {
                const goal = goalMatch[1].trim();
                const description = `Run ${goal} goal`;
                goals.push({ goal, description });
                console.log(`[Maven Plugin Discovery] Found goal (pattern 4): ${goal} - ${description}`);
                continue;
            }

            // Stop if we hit obvious end markers
            if (trimmedLine.includes('===') ||
                trimmedLine.toLowerCase().includes('for more information') ||
                trimmedLine.startsWith('[INFO]') ||
                trimmedLine.startsWith('[ERROR]')) {
                console.log(`[Maven Plugin Discovery] Stopping parsing at line ${i}: "${trimmedLine}"`);
                break;
            }
        }
    }

    console.log(`[Maven Plugin Discovery] Parsing complete - foundGoalsHeader: ${foundGoalsHeader}, goals found: ${goals.length}`);

    // If we didn't find any goals with the header approach, try a more aggressive search
    if (goals.length === 0) {
        console.log(`[Maven Plugin Discovery] No goals found with header approach, trying alternative parsing`);
        for (const line of lines) {
            const trimmedLine = line.trim();
            if (trimmedLine.includes(':') && trimmedLine.includes('goal')) {
                const parts = trimmedLine.split(':');
                if (parts.length >= 4) {
                    const goal = parts[3].split(/\s+/)[0];
                    if (goal && goal.length > 1) {
                        goals.push({ goal, description: `Run ${goal} goal` });
                        console.log(`[Maven Plugin Discovery] Found goal (alternative): ${goal}`);
                    }
                }
            }
        }
    }

    return goals;
}

function getTargetName(artifactId: string, goal: string): string {
    // Remove common prefixes/suffixes to create cleaner target names
    const cleanArtifactId = artifactId
        .replace(/-maven-plugin$/, '')
        .replace(/^maven-/, '')
        .replace(/^spring-boot-/, 'boot-');

    return goal === 'run' || goal === 'dev' ? goal : `${cleanArtifactId}-${goal}`;
}

function getPluginTechnology(artifactId: string): string {
    if (artifactId.includes('quarkus')) return 'quarkus';
    if (artifactId.includes('spring-boot')) return 'spring-boot';
    if (artifactId.includes('flyway')) return 'flyway';
    if (artifactId.includes('liquibase')) return 'liquibase';
    if (artifactId.includes('spotless')) return 'spotless';
    if (artifactId.includes('checkstyle')) return 'checkstyle';
    if (artifactId.includes('spotbugs')) return 'spotbugs';
    return artifactId;
}

function shouldCacheTarget(goal: string): boolean {
    // Development goals should not be cached
    const devGoals = ['run', 'dev', 'start', 'stop', 'migrate', 'update'];
    return !devGoals.includes(goal);
}

function getInputsForPlugin(pluginKey: string, goal: string, projectRoot: string): string[] {
    const baseInputs = ['{projectRoot}/src/**/*', '{projectRoot}/pom.xml'];

    // Add plugin-specific inputs
    if (pluginKey.includes('flyway')) {
        return [...baseInputs, '{projectRoot}/src/main/resources/db/migration/**/*'];
    }
    if (pluginKey.includes('liquibase')) {
        return [...baseInputs, '{projectRoot}/src/main/resources/db/changelog/**/*'];
    }

    return baseInputs;
}

function getOutputsForPlugin(pluginKey: string, goal: string, projectRoot: string): string[] | undefined {
    // Development goals typically don't have meaningful outputs
    const devGoals = ['run', 'dev', 'start', 'stop', 'migrate', 'update'];
    if (devGoals.includes(goal)) {
        return undefined;
    }

    // Plugin-specific outputs
    if (pluginKey.includes('checkstyle')) {
        return ['{projectRoot}/target/checkstyle-result.xml'];
    }
    if (pluginKey.includes('spotbugs')) {
        return ['{projectRoot}/target/spotbugsXml.xml'];
    }

    return ['{projectRoot}/target/**/*'];
}

function getProjectInputs(projectInfo: MavenProjectInfo, projectRoot: string): string[] {
    const inputs = ['default', '^production'];

    // Add source directories
    inputs.push('{projectRoot}/src/main/**/*');

    // Add test directories if they exist
    inputs.push('{projectRoot}/src/test/**/*');

    // Add resources
    inputs.push('{projectRoot}/src/main/resources/**/*');

    return inputs;
}

function getProjectOutputs(projectInfo: MavenProjectInfo, projectRoot: string): string[] {
    return [
        '{projectRoot}/target/**/*',
    ];
}


// Optimized version using coordinate map instead of linear search
function createProjectDependenciesOptimized(
    projectInfo: MavenProjectInfo,
    coordinateMap: Map<string, string>,
    includeTestDependencies: boolean = false
): string[] {
    if (!projectInfo.dependencies) return [];

    const dependencies: string[] = [];

    for (const dep of projectInfo.dependencies) {
        // Include compile, runtime, and optionally test dependencies
        const includeThisDep = dep.scope === 'compile' ||
            dep.scope === 'runtime' ||
            !dep.scope || // Default scope is compile
            (includeTestDependencies && dep.scope === 'test');

        if (!includeThisDep) continue;

        // Fast coordinate lookup
        const coordinate = `${dep.groupId}.${dep.artifactId}`;
        const dependencyProjectName = coordinateMap.get(coordinate);
        if (dependencyProjectName) {
            dependencies.push(dependencyProjectName);
        }
    }

    return dependencies;
}

function createProjectDependencies(
    projectInfo: MavenProjectInfo,
    mavenReport: MavenReport,
    includeTestDependencies: boolean = false
): string[] {
    if (!projectInfo.dependencies) return [];

    const dependencies: string[] = [];

    for (const dep of projectInfo.dependencies) {
        // Include compile, runtime, and optionally test dependencies
        const includeThisDep = dep.scope === 'compile' ||
            dep.scope === 'runtime' ||
            !dep.scope || // Default scope is compile
            (includeTestDependencies && dep.scope === 'test');

        if (!includeThisDep) continue;

        // Try to find a matching project in the workspace
        const dependencyProjectName = findProjectByMavenCoordinates(dep.groupId, dep.artifactId, mavenReport);
        if (dependencyProjectName) {
            dependencies.push(dependencyProjectName);
        }
    }

    return dependencies;
}

function createImplicitDependencies(
    projectInfo: MavenProjectInfo,
    mavenReport: MavenReport,
    pomFilePath: string
): string[] {
    const implicitDeps: string[] = [];

    // Add parent as implicit dependency
    if (projectInfo.parent) {
        const parentProjectName = findProjectByMavenCoordinates(
            projectInfo.parent.groupId,
            '', // Parent might not have artifactId in our data structure
            mavenReport
        );
        if (parentProjectName) {
            implicitDeps.push(parentProjectName);
        }
    }

    // Add modules as implicit dependencies (for aggregator projects)
    if (projectInfo.modules) {
        const projectRoot = dirname(pomFilePath);
        for (const module of projectInfo.modules) {
            const modulePath = join(projectRoot, module, 'pom.xml');
            const moduleProject = mavenReport.projects.get(modulePath);
            if (moduleProject) {
                const moduleProjectName = `${moduleProject.groupId ?? moduleProject.parent?.groupId}.${moduleProject.artifactId}`;
                implicitDeps.push(moduleProjectName);
            }
        }
    }

    return implicitDeps;
}

function findProjectByMavenCoordinates(
    groupId: string,
    artifactId: string,
    mavenReport: MavenReport
): string | null {
    for (const [pomPath, project] of mavenReport.projects) {
        const projectGroupId = project.groupId ?? project.parent?.groupId;
        if (projectGroupId === groupId && project.artifactId === artifactId) {
            return `${projectGroupId}.${project.artifactId}`;
        }
    }
    return null;
}

function determineProjectType(projectInfo: MavenProjectInfo): 'application' | 'library' {
    // POM packaging usually indicates an aggregator/parent project
    if (projectInfo.packaging === 'pom') {
        return 'library';
    }

    // Common application packaging types
    if (['jar', 'war', 'ear'].includes(projectInfo.packaging)) {
        // Check if it has a main class or is a web application
        if (projectInfo.packaging === 'war' || projectInfo.packaging === 'ear') {
            return 'application';
        }

        // For JAR, we need to determine if it's a library or application
        // Look for common patterns that indicate an application
        const isApplication = projectInfo.dependencies?.some(dep =>
            // Spring Boot applications
            (dep.groupId === 'org.springframework.boot' && dep.artifactId === 'spring-boot-starter') ||
            // Quarkus applications
            (dep.groupId === 'io.quarkus' && dep.artifactId.includes('quarkus-')) ||
            // Main class indicators
            dep.artifactId.includes('main') || dep.artifactId.includes('app')
        ) ?? false;

        return isApplication ? 'application' : 'library';
    }

    return 'library';
}

function splitConfigFiles(files: readonly string[]) {
    const pomFiles = files
        .filter((f) => f.endsWith('pom.xml'))
        .filter((f) => !isTestOrTemplateDirectory(f));
    const testFiles = files.filter((f) => f.includes('/src/test/'));
    return {pomFiles, testFiles};
}

function isTestOrTemplateDirectory(filePath: string): boolean {
    const excludePatterns = [
        '/src/test/resources',
        '/maven-archetype/',
        '/resources-filtered/',
        '/test/resources',
        'archetype-resources',
        '/templates/',
    ];

    return excludePatterns.some(pattern => filePath.includes(pattern));
}

function getMavenProjectRootToTestFilesMap(
    testFiles: string[],
    projectRoots: string[]
): Record<string, string[]> {
    const map: Record<string, string[]> = {};
    for (const root of projectRoots) {
        map[root] = testFiles.filter((f) => f.startsWith(root));
    }
    return map;
}

// Plugins that are common but don't provide useful goals for build automation
const SKIP_DISCOVERY_PLUGINS = new Set([
    'org.apache.maven.plugins:maven-clean-plugin',
    'org.apache.maven.plugins:maven-resources-plugin',
    'org.apache.maven.plugins:maven-jar-plugin',
    'org.apache.maven.plugins:maven-install-plugin',
    'org.apache.maven.plugins:maven-deploy-plugin',
    'org.apache.maven.plugins:maven-site-plugin',
    'org.apache.maven.plugins:maven-project-info-reports-plugin',
    'org.apache.maven.plugins:maven-dependency-plugin',
    'org.codehaus.mojo:versions-maven-plugin',
    'org.jacoco:jacoco-maven-plugin', // Coverage reports not useful as targets
    'org.apache.maven.plugins:maven-gpg-plugin', // Signing plugin
    'org.sonatype.plugins:nexus-staging-maven-plugin', // Release plugin
]);

function collectUniquePlugins(mavenReport: MavenReport, pomFiles: string[]): Set<string> {
    const uniquePlugins = new Set<string>();
    let totalPluginCount = 0;
    let knownPluginCount = 0;
    let skippedPluginCount = 0;

    for (const pomFile of pomFiles) {
        const projectInfo = mavenReport.projects.get(pomFile);
        if (!projectInfo?.plugins) continue;

        for (const plugin of projectInfo.plugins) {
            totalPluginCount++;
            const pluginKey = `${plugin.groupId}:${plugin.artifactId}`;

            if (PLUGIN_CONFIGS.has(pluginKey)) {
                knownPluginCount++;
            } else if (SKIP_DISCOVERY_PLUGINS.has(pluginKey)) {
                skippedPluginCount++;
            } else {
                // Only collect unknown plugins that are worth discovering
                uniquePlugins.add(pluginKey);
            }
        }
    }

    console.log(`[Maven Plugin] Plugin summary: ${totalPluginCount} total, ${knownPluginCount} known, ${skippedPluginCount} skipped, ${uniquePlugins.size} unique unknown plugins to discover`);
    console.log(`[Maven Plugin] Unknown plugins:`, Array.from(uniquePlugins).join(', '));

    return uniquePlugins;
}

async function batchDiscoverPlugins(
    uniquePlugins: Set<string>,
    workspaceRoot: string
): Promise<Map<string, DiscoveredPlugin>> {
    const discoveredPlugins = new Map<string, DiscoveredPlugin>();

    if (uniquePlugins.size === 0) {
        console.log(`[Maven Plugin] No unknown plugins to discover`);
        return discoveredPlugins;
    }

    console.log(`[Maven Plugin] Starting parallel discovery of ${uniquePlugins.size} unique plugins...`);
    const startTime = Date.now();

    // Discover all plugins in parallel using Promise.allSettled
    const pluginArray = Array.from(uniquePlugins);
    const discoveryPromises = pluginArray.map(async (pluginKey) => {
        const discovered = await pluginDiscoveryPool.discover(pluginKey, workspaceRoot);
        return { pluginKey, discovered };
    });

    const results = await Promise.allSettled(discoveryPromises);

    // Process results
    for (const result of results) {
        if (result.status === 'fulfilled' && result.value.discovered) {
            discoveredPlugins.set(result.value.pluginKey, result.value.discovered);
        }
    }

    const discoveryTime = Date.now() - startTime;
    console.log(`[Maven Plugin] Discovered ${discoveredPlugins.size}/${uniquePlugins.size} plugins in ${discoveryTime}ms`);

    return discoveredPlugins;
}

async function discoverMavenModules(workspaceRoot: string): Promise<string[]> {
    const discoveredPoms: string[] = [];
    const processed = new Set<string>();

    async function processModule(pomPath: string) {
        // Convert to absolute path if not already absolute
        const absolutePomPath = pomPath.startsWith('/') || pomPath.includes(':')
            ? pomPath
            : join(workspaceRoot, pomPath);

        console.log(`[Maven Plugin] Processing module: ${pomPath} -> ${absolutePomPath}`);

        if (processed.has(absolutePomPath)) {
            console.log(`[Maven Plugin] Already processed: ${absolutePomPath}`);
            return;
        }

        if (!existsSync(absolutePomPath)) {
            console.log(`[Maven Plugin] File does not exist: ${absolutePomPath}`);
            return;
        }

        processed.add(absolutePomPath);
        discoveredPoms.push(absolutePomPath);

        try {
            // Parse this pom.xml to get its modules (use raw POM for module discovery)
            const miniReport = await parsePomXml(workspaceRoot, [absolutePomPath], false);
            const projectInfo = miniReport.projects.get(absolutePomPath);

            if (projectInfo?.modules) {
                const currentDir = dirname(absolutePomPath);
                console.log(`[Maven Plugin] Found ${projectInfo.modules.length} modules in ${absolutePomPath}`);

                // Process each module recursively
                for (const module of projectInfo.modules) {
                    const modulePomPath = join(currentDir, module, 'pom.xml');
                    await processModule(modulePomPath);
                }
            }
        } catch (error: any) {
            console.warn(`[Maven Plugin] Error processing ${absolutePomPath}:`, error?.message);
        }
    }

    // Start with root pom.xml
    const rootPomPath = join(workspaceRoot, 'pom.xml');
    console.log(`[Maven Plugin] Starting module discovery from ${rootPomPath}`);

    await processModule(rootPomPath);

    console.log(`[Maven Plugin] Module discovery complete. Found ${discoveredPoms.length} Maven projects`);
    return discoveredPoms;
}

export {writeTargetsToCache};
