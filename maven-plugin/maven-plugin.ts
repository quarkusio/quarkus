import {
  CreateDependencies,
  CreateNodesV2,
  CreateNodesContextV2,
  CreateNodesResultV2,
  workspaceRoot,
  readJsonFile,
  writeJsonFile,
} from '@nx/devkit';
import { dirname, join } from 'path';
import { existsSync, readFileSync } from 'fs';
import { spawn } from 'child_process';
import { workspaceDataDirectory } from 'nx/src/utils/cache-directory';
import { calculateHashForCreateNodes } from '@nx/devkit/src/utils/calculate-hash-for-create-nodes';

export interface MavenPluginOptions {
  mavenExecutable?: string;
  verbose?: boolean;
}

const DEFAULT_OPTIONS: MavenPluginOptions = {
  mavenExecutable: 'mvn',
};

// Global cache to avoid running Maven analysis multiple times
let globalAnalysisCache: any = null;
let globalCacheKey: string | null = null;

// Cache management functions
function readMavenCache(cachePath: string): Record<string, any> {
  try {
    return existsSync(cachePath) ? readJsonFile(cachePath) : {};
  } catch {
    return {};
  }
}

function writeMavenCache(cachePath: string, cache: Record<string, any>) {
  try {
    writeJsonFile(cachePath, cache);
  } catch (error) {
    console.warn('Failed to write Maven cache:', error.message);
  }
}

/**
 * Maven plugin that delegates to Java for analysis and returns results directly
 */
export const createNodesV2: CreateNodesV2 = [
  '**/pom.xml',
  async (configFiles, options, context): Promise<CreateNodesResultV2> => {
    const opts: MavenPluginOptions = { ...DEFAULT_OPTIONS, ...(options as MavenPluginOptions) };

    if (opts.verbose) {
      console.log(`Maven plugin found ${configFiles.length} pom.xml files`);
    }

    // Filter out unwanted pom.xml files
    const filteredFiles = configFiles.filter(file =>
      !file.includes('maven-script/') &&
      !file.includes('target/') &&
      !file.includes('node_modules/')
    );

    if (filteredFiles.length === 0) {
      return [];
    }

    // Generate cache key based on pom.xml files and options
    const projectHash = await calculateHashForCreateNodes(
      workspaceRoot,
      (options as object) ?? {},
      context,
      ['{projectRoot}/pom.xml', '{workspaceRoot}/**/pom.xml']
    );
    const cacheKey = projectHash;

    // OPTIMIZATION: Check global in-memory cache first
    if (globalAnalysisCache && globalCacheKey === cacheKey) {
      if (opts.verbose) {
        console.log('Using global in-memory cache for createNodes');
      }
      return globalAnalysisCache.createNodesResults || [];
    }

    // Set up cache path
    const cachePath = join(workspaceDataDirectory, 'maven-analysis-cache.json');
    const cache = readMavenCache(cachePath);

    // Check if we have valid cached results
    if (cache[cacheKey]) {
      if (opts.verbose) {
        console.log('Using cached Maven analysis results for createNodes');
      }
      // Store in global cache for faster subsequent access
      globalAnalysisCache = cache[cacheKey];
      globalCacheKey = cacheKey;
      return cache[cacheKey].createNodesResults || [];
    }

    // Run analysis if not cached
    const result = await runMavenAnalysis(opts);

    // Cache the complete result
    cache[cacheKey] = result;
    writeMavenCache(cachePath, cache);

    // Store in global cache
    globalAnalysisCache = result;
    globalCacheKey = cacheKey;

    return result.createNodesResults || [];
  },
];

/**
 * Create dependencies using Java analysis results
 */
export const createDependencies: CreateDependencies = async (options, context) => {
  const opts: MavenPluginOptions = { ...DEFAULT_OPTIONS, ...(options as MavenPluginOptions) };

  try {
    // Generate cache key based on pom.xml files and options
    const projectHash = await calculateHashForCreateNodes(
      workspaceRoot,
      (options as object) ?? {},
      context,
      ['{projectRoot}/pom.xml', '{workspaceRoot}/**/pom.xml']
    );
    const cacheKey = projectHash;

    // OPTIMIZATION: Check global in-memory cache first
    if (globalAnalysisCache && globalCacheKey === cacheKey) {
      if (opts.verbose) {
        console.log('Using global in-memory cache for createDependencies');
      }
      return globalAnalysisCache.createDependencies || [];
    }

    // Set up cache path
    const cachePath = join(workspaceDataDirectory, 'maven-analysis-cache.json');
    const cache = readMavenCache(cachePath);

    // Check if we have valid cached results
    if (cache[cacheKey]) {
      if (opts.verbose) {
        console.log('Using cached Maven analysis results for createDependencies');
      }
      // Store in global cache for faster subsequent access
      globalAnalysisCache = cache[cacheKey];
      globalCacheKey = cacheKey;
      return cache[cacheKey].createDependencies || [];
    }

    // Run analysis if not cached - this should rarely happen since createNodesV2 runs first
    const result = await runMavenAnalysis(opts);

    // Cache the complete result
    cache[cacheKey] = result;
    writeMavenCache(cachePath, cache);

    // Store in global cache
    globalAnalysisCache = result;
    globalCacheKey = cacheKey;

    return result.createDependencies || [];

  } catch (error) {
    console.error(`Maven dependency analysis failed:`, error.message);
    return [];
  }
};

/**
 * Run Maven analysis using Java plugin
 */
async function runMavenAnalysis(options: MavenPluginOptions): Promise<any> {
  const outputFile = join(workspaceDataDirectory, 'maven-analysis.json');

  // Check if verbose mode is enabled
  const isVerbose = options.verbose || process.env.NX_VERBOSE_LOGGING === 'true' || process.argv.includes('--verbose');

  // Check if Java analyzer is available
  if (!findJavaAnalyzer()) {
    throw new Error('Maven analyzer not found. Please ensure maven-plugin is compiled.');
  }

  if (isVerbose) {
    console.log(`Running Maven analysis with verbose logging enabled...`);
    console.log(`Maven executable: ${options.mavenExecutable}`);
    console.log(`Output file: ${outputFile}`);
  }

  // Build Maven command arguments
  // Use the custom nx:analyze goal from our Java Maven plugin
  const mavenArgs = [
    'io.quarkus:maven-plugin:999-SNAPSHOT:analyze',
    `-Dnx.outputFile=${outputFile}`,
    `-Dnx.verbose=${isVerbose}`
  ];

  // Always use quiet mode to suppress expected reactor dependency warnings
  // These warnings are normal in large multi-module projects and don't affect functionality

  if (isVerbose) {
    console.log(`Executing Maven command: ${options.mavenExecutable} ${mavenArgs.join(' ')}`);
    console.log(`Working directory: ${workspaceRoot}`);
  } else {
    mavenArgs.push('-q');
  }

  // Run Maven plugin
  await new Promise<void>((resolve, reject) => {
    const child = spawn(options.mavenExecutable, mavenArgs, {
      cwd: workspaceRoot,
      stdio: 'inherit'
    });

    // No timeout for very large codebases like Quarkus
    child.on('close', (code) => {
      if (isVerbose) {
        console.log(`Maven process completed with exit code: ${code}`);
      }
      if (code === 0) {
        if (isVerbose) {
          console.log(`Maven analysis completed successfully`);
        }
        resolve();
      } else {
        reject(new Error(`Maven process exited with code ${code}`));
      }
    });

    child.on('error', (error) => {
      reject(new Error(`Failed to spawn Maven process: ${error.message}`));
    });
  });

  // Read and return JSON result
  if (!existsSync(outputFile)) {
    throw new Error(`Output file not found: ${outputFile}`);
  }

  const jsonContent = readFileSync(outputFile, 'utf8');
  return JSON.parse(jsonContent);
}

/**
 * Find the compiled Java Maven analyzer
 */
function findJavaAnalyzer(): string | null {
  const possiblePaths = [
    join(__dirname, 'target/classes'),
    join(__dirname, 'target/maven-plugin-999-SNAPSHOT.jar'),
  ];

  for (const path of possiblePaths) {
    if (existsSync(path)) {
      return path;
    }
  }

  return null;
}


/**
 * Plugin configuration
 */
export default {
  name: 'maven-plugin',
  createNodesV2,
  createDependencies,
};
